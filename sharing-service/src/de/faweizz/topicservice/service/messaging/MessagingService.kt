package de.faweizz.topicservice.service.messaging

import de.faweizz.topicservice.service.joined.execution.CodeExecutionPoll
import de.faweizz.topicservice.service.joined.resource.JoinedResource
import de.faweizz.topicservice.service.joined.resource.JoinedResourceJoinRequest
import de.faweizz.topicservice.service.model.ResourceSharingRequest

interface MessagingService {

    fun sendResourceSharingRequest(resourceSharingRequest: ResourceSharingRequest)
    fun sendJoinedResourceJoinRequest(request: JoinedResourceJoinRequest)
    fun sendCodeExecutionPoll(resource: JoinedResource, poll: CodeExecutionPoll)
}