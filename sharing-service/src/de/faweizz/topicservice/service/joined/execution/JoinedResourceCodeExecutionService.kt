package de.faweizz.topicservice.service.joined.execution

import de.faweizz.topicservice.Actor
import de.faweizz.topicservice.persistence.Mongo
import de.faweizz.topicservice.service.auditlogging.AuditLogger
import de.faweizz.topicservice.service.client.ClientService
import de.faweizz.topicservice.service.joined.resource.CodeExecution
import de.faweizz.topicservice.service.messaging.MessagingService
import de.faweizz.topicservice.service.polling.PollingService
import de.faweizz.topicservice.service.resource.AccessLevel
import de.faweizz.topicservice.service.resource.ResourceService
import org.apache.avro.Schema
import java.util.*

class JoinedResourceCodeExecutionService(
    private val mongo: Mongo,
    private val messagingService: MessagingService,
    private val resourceService: ResourceService,
    private val clientService: ClientService,
    private val deploymentService: DeploymentService,
    private val auditLogger: AuditLogger
) : PollingService<CodeExecutionPoll> {

    fun createCodeExecutionRequest(actor: Actor, codeExecutionRequest: CodeExecutionRequest): CodeExecutionPoll {
        val resource = mongo.findJoinedResourceByName(codeExecutionRequest.resourceName)
            ?: throw Exception("Unknown resource")

        if (!resource.owningActorIds.contains(actor.name)) throw Exception("Actor not owner of shared resource")

        val poll = CodeExecutionPoll(
            requester = actor.name,
            resourceName = resource.resourceId,
            comment = codeExecutionRequest.comment,
            name = codeExecutionRequest.name,
            gitUrl = codeExecutionRequest.gitUrl,
            commitHash = codeExecutionRequest.commitHash,
            actorsToAccept = (resource.owningActorIds - listOf(actor.name)).toSet()
        )

        mongo.saveCodeExecutionPoll(poll)
        messagingService.sendCodeExecutionPoll(resource, poll)

        auditLogger.logJoinedResourceComputeRequest(poll.id, actor.name, poll.actorsToAccept.toList())

        return poll
    }

    override fun getPollableById(pollableId: String): CodeExecutionPoll {
        return mongo.findCodeExecutionPollById(pollableId) ?: throw Exception("Unknown poll")
    }

    override fun updatePollable(pollable: CodeExecutionPoll) {
        mongo.updateCodeExecutionPollById(pollable)
    }

    override suspend fun onAccepted(pollable: CodeExecutionPoll) {
        val joinedResource =
            mongo.findJoinedResourceByName(pollable.resourceName) ?: throw Exception("Unknown resource")
        val targetResourceName = "${pollable.resourceName}_${pollable.name}_${pollable.commitHash}"
        val targetResource = resourceService.createResource(
            fullyQualifiedName = targetResourceName,
            owningActorIds = joinedResource.owningActorIds,
            accessLevel = AccessLevel.READ,
            schema = Schema.Parser().parse(joinedResource.schema),
            description = "Target-Resource for custom code execution ${pollable.name}"
        )

        val codeExecutionClient =
            clientService.createClient(
                UUID.randomUUID().toString(),
                listOf(pollable.resourceName),
                listOf(targetResource.id)
            )

        val codeExecution = CodeExecution(
            name = pollable.name,
            submittedByActor = pollable.requester,
            gitlabRepositoryUrl = pollable.gitUrl,
            commitHash = pollable.commitHash,
            clientName = codeExecutionClient.name,
            clientSecret = codeExecutionClient.secret,
            clientConsumerGroup = codeExecutionClient.consumerGroup,
            inputTopic = pollable.resourceName,
            outputTopic = targetResourceName
        )

        joinedResource.codeExecutions += codeExecution

        mongo.updateJoinedResource(joinedResource)
        deploymentService.deploy(codeExecution)
    }

    override suspend fun accept(actor: Actor, pollableId: String): CodeExecutionPoll {
        val accept = super.accept(actor, pollableId)
        auditLogger.logJoinedResourceComputeRequestAccept(pollableId, actor.name)
        return accept
    }

    override fun decline(actor: Actor, pollableId: String): CodeExecutionPoll {
        val decline = super.decline(actor, pollableId)
        auditLogger.logJoinedResourceComputeRequestDecline(pollableId, actor.name)
        return decline
    }
}