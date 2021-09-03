package de.faweizz.topicservice.service.sharing

import de.faweizz.topicservice.Actor
import de.faweizz.topicservice.persistence.Mongo
import de.faweizz.topicservice.service.auditlogging.AuditLogger
import de.faweizz.topicservice.service.messaging.MessagingService
import de.faweizz.topicservice.service.model.ResourceSharingRequest
import de.faweizz.topicservice.service.polling.PollingService
import de.faweizz.topicservice.service.resource.AccessLevel
import de.faweizz.topicservice.service.resource.ResourceService
import de.faweizz.topicservice.service.transformation.TransformationExecutor
import de.faweizz.topicservice.service.transformation.TransformationService
import de.faweizz.topicservice.service.transformation.TransformationStepData
import de.faweizz.topicservice.service.transformation.step.TransformationStepFactory
import org.apache.avro.Schema

class ResourceSharingService(
    private val mongo: Mongo,
    private val messagingService: MessagingService,
    private val resourceService: ResourceService,
    private val transformationService: TransformationService,
    private val auditLogger: AuditLogger
) : PollingService<ResourceSharingRequest> {

    fun requestSharing(resourceId: String, actor: Actor): ResourceSharingRequest {
        val resource = mongo.findResourceById(resourceId)
            ?: throw Exception("Requested topic $resourceId unknown")

        if (resource.isOwner(actor)) throw TenantIsOwnerOfTopicException(
            actor.name,
            resource.id
        )

        val resourceSharingRequest = ResourceSharingRequest(
            resourceId = resource.id,
            requestingActorId = actor.name,
            actorsToAccept = resource.owningActors.toSet()
        )

        mongo.saveResourceSharingRequest(resourceSharingRequest)
        messagingService.sendResourceSharingRequest(resourceSharingRequest)

        auditLogger.logDataSharingRequest(
            resourceSharingRequest.id,
            resourceId,
            resourceSharingRequest.actorsToAccept.toList()
        )

        return resourceSharingRequest
    }

    override fun getPollableById(pollableId: String): ResourceSharingRequest {
        return mongo.findResourceSharingRequestById(pollableId)
            ?: throw Exception("Unknown ResourceSharingRequest $pollableId")
    }

    override fun updatePollable(pollable: ResourceSharingRequest) {
        mongo.updateTopicSharingRequest(pollable)
    }

    fun addTransformation(
        requestId: String,
        transformationStepAddRequest: TransformationStepAddRequest,
        actor: Actor
    ): ResourceSharingRequest {
        val request = mongo.findResourceSharingRequestById(requestId)
            ?: throw Exception("Unknown poll")
        if (!request.canAccept(actor.name)) throw Exception("Actor can't modify request")

        val step = TransformationStepData(
            transformationStepAddRequest.transformationStepId,
            transformationStepAddRequest.data
        )

        TransformationStepFactory().instantiate(step).validate()

        request.transformationSteps += PendingTransformationStep(step = step)

        mongo.updateTopicSharingRequest(request)
        return request
    }

    override suspend fun onAccepted(pollable: ResourceSharingRequest) {
        val mirrorResourceName = "${pollable.requestingActorId}.mirror.${pollable.resourceId}"
        val sourceResource = mongo.findResourceById(pollable.resourceId) ?: throw Exception("Unknown resource id")

        val requiresMirrorResource = pollable.transformationSteps.isNotEmpty()

        val transformation = if (requiresMirrorResource) {
            val sourceResourceSchema = Schema.Parser().parse(sourceResource.schema)
            val transformationExecutor = TransformationExecutor(pollable.transformationSteps.map { it.step })
            val targetResourceSchema = transformationExecutor.calculateSchema(sourceResourceSchema)

            val mirrorResource = resourceService.createResource(
                mirrorResourceName,
                listOf(pollable.requestingActorId),
                schema = targetResourceSchema,
                description = "Mirror resource for ${sourceResource.id}"
            )

            transformationService.createTransformation(
                pollable.resourceId,
                mirrorResource.id,
                transformationStepData = pollable.transformationSteps.map {
                    TransformationStepData(
                        it.step.transformationStepName,
                        it.step.data
                    )
                })
        } else null

        val transaction = Transaction(
            resourceId = pollable.resourceId,
            sharedWithActorId = pollable.requestingActorId,
            acceptedByActors = pollable.acceptedActors.toList(),
            transformationId = transformation?.id,
            grantedAccessLevel = if (requiresMirrorResource) AccessLevel.NONE else AccessLevel.READ
        )

        mongo.saveTransaction(transaction)
    }

    override suspend fun accept(actor: Actor, pollableId: String): ResourceSharingRequest {
        val accept = super.accept(actor, pollableId)
        auditLogger.logDataSharingRequestAccept(pollableId, actor.name)
        return accept
    }

    override fun decline(actor: Actor, pollableId: String): ResourceSharingRequest {
        val decline = super.decline(actor, pollableId)
        auditLogger.logDataSharingRequestDecline(pollableId, actor.name)
        return decline
    }
}