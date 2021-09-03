package de.faweizz.topicservice.service.joined.resource

data class JoinedResourceCreationRequest(
    val name: String,
    val sourceResource: String,
    val description: String
)
