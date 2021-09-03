package de.faweizz.topicservice.service.messaging

import de.faweizz.topicservice.service.joined.execution.CodeExecutionPoll
import de.faweizz.topicservice.service.joined.resource.JoinedResource
import de.faweizz.topicservice.service.joined.resource.JoinedResourceJoinRequest
import de.faweizz.topicservice.service.model.ResourceSharingRequest

class StubMessagingService : MessagingService {
    override fun sendResourceSharingRequest(resourceSharingRequest: ResourceSharingRequest) {
        println("Messaging: $resourceSharingRequest")
    }

    override fun sendJoinedResourceJoinRequest(request: JoinedResourceJoinRequest) {
        println("Messaging: $request")
    }

    override fun sendCodeExecutionPoll(resource: JoinedResource, poll: CodeExecutionPoll) {
        println("Messaging: $resource")
    }
}