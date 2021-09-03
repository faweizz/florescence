package de.faweizz.topicservice.service.joined.resource

data class JoinedTopicJoinRequestRequest(
    val joinedTopicName: String,
    val sourceTopicName: String
)