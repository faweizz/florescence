package de.faweizz.topicservice.service.joined.resource

import de.faweizz.topicservice.Actor
import de.faweizz.topicservice.adjacent.atlas.AtlasClient
import de.faweizz.topicservice.adjacent.kafka.KafkaClient
import de.faweizz.topicservice.persistence.Mongo
import de.faweizz.topicservice.service.auditlogging.AuditLogger
import de.faweizz.topicservice.service.messaging.MessagingService
import de.faweizz.topicservice.service.polling.PollingService
import de.faweizz.topicservice.service.sharing.Transaction
import de.faweizz.topicservice.service.transformation.TransformationService
import org.apache.avro.Schema

class JoinedResourceService(
    private val mongo: Mongo,
    private val atlasClient: AtlasClient,
    private val kafkaClient: KafkaClient,
    private val messagingService: MessagingService,
    private val transformationService: TransformationService,
    private val auditLogger: AuditLogger
) : PollingService<JoinedResourceJoinRequest> {

    suspend fun create(joinedResourceCreationRequest: JoinedResourceCreationRequest, actor: Actor): JoinedResource {
        val sourceResource = mongo.findResourceById(joinedResourceCreationRequest.sourceResource)
            ?: throw Exception("Unknown source resource")

        if (!sourceResource.isOwner(actor)) throw Exception("Cant create joined resource with unknown source topic")

        val joinedResource = JoinedResource(
            resourceId = "joined_${joinedResourceCreationRequest.name}",
            owningActorIds = mutableListOf(actor.name),
            schema = sourceResource.schema,
            description = joinedResourceCreationRequest.description
        )

        if (mongo.findJoinedResourceByName(joinedResource.resourceId) != null) throw Exception("Joined resource already exists")
        kafkaClient.createTopic(joinedResource.resourceId)
        atlasClient.createResourceEntity(joinedResource.resourceId, joinedResource.schema, joinedResource.description)

        mongo.saveJoinedResource(joinedResource)

        val transaction = Transaction(
            resourceId = joinedResourceCreationRequest.sourceResource,
            sharedWithActorId = null,
            acceptedByActors = listOf(actor.name),
            transformationId = transformationService.createTransformation(
                joinedResourceCreationRequest.sourceResource,
                joinedResource.resourceId
            ).id
        )

        mongo.saveTransaction(transaction)

        auditLogger.logJoinedResourceCreation(joinedResource.resourceId, actor.name)

        return joinedResource
    }

    fun createJoinRequest(
        joinedTopicJoinRequestRequest: JoinedTopicJoinRequestRequest,
        actor: Actor
    ): JoinedResourceJoinRequest {
        val joinedResource =
            mongo.findJoinedResourceByName(joinedTopicJoinRequestRequest.joinedTopicName)
                ?: throw Exception("Joined resource ${joinedTopicJoinRequestRequest.joinedTopicName} does not exist")

        val sourceResource =
            mongo.findResourceById(joinedTopicJoinRequestRequest.sourceTopicName)
                ?: throw Exception("Source resource ${joinedTopicJoinRequestRequest.sourceTopicName} does not exist")

        if (!sourceResource.isOwner(actor)) throw Exception("Requesting actor is not owner of requested source Resource")

        val joinedResourceSchema = Schema.Parser().parse(joinedResource.schema)
        val sourceResourceSchema = Schema.Parser().parse(sourceResource.schema)

        if (joinedResourceSchema != sourceResourceSchema) throw Exception("Schemas do not match")

        val request = JoinedResourceJoinRequest(
            sourcedResource = sourceResource.id,
            targetJoinedResource = joinedResource.resourceId,
            requestingActor = actor.name,
            actorsToAccept = joinedResource.owningActorIds.toSet()
        )

        mongo.saveJoinedResourceJoinRequest(request)
        messagingService.sendJoinedResourceJoinRequest(request)

        auditLogger.logJoinedResourceJoinRequest(
            request.id,
            request.targetJoinedResource,
            actor.name,
            request.actorsToAccept.toList()
        )

        return request
    }

    override suspend fun accept(actor: Actor, pollableId: String): JoinedResourceJoinRequest {
        val accept = super.accept(actor, pollableId)
        auditLogger.logJoinedResourceJoinRequestAccept(pollableId, actor.name)
        return accept
    }

    override fun decline(actor: Actor, pollableId: String): JoinedResourceJoinRequest {
        val decline = super.decline(actor, pollableId)
        auditLogger.logJoinedResourceJoinRequestDecline(pollableId, actor.name)
        return decline
    }

    override fun getPollableById(pollableId: String): JoinedResourceJoinRequest {
        return mongo.findJoinedResourceJoinRequest(pollableId)
            ?: throw Exception("Unknown JoinedResourceJoinRequest $pollableId")
    }

    override fun updatePollable(pollable: JoinedResourceJoinRequest) {
        mongo.updateJoinedResourceJoinRequest(pollable)
    }

    override suspend fun onAccepted(pollable: JoinedResourceJoinRequest) {
        val joinedResource = mongo.findJoinedResourceByName(pollable.targetJoinedResource)
            ?: throw Exception("Can't find resource ${pollable.targetJoinedResource}")
        val sourceResource = mongo.findResourceById(pollable.sourcedResource)
            ?: throw Exception("Can't find resource ${pollable.sourcedResource}")

        joinedResource.owningActorIds += pollable.requestingActor

        mongo.updateSharedResource(joinedResource)

        val transaction = Transaction(
            resourceId = sourceResource.id,
            sharedWithActorId = null,
            acceptedByActors = pollable.acceptedActors.toList(),
            transformationId = transformationService.createTransformation(
                sourceResource.id,
                joinedResource.resourceId
            ).id
        )

        mongo.saveTransaction(transaction)
    }
}