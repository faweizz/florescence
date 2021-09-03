package de.faweizz.topicservice.service.resource

data class TopicCreationRequest(
    val topicName: String,
    val schema: String,
    val description: String
)
