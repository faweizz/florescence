package de.faweizz.poc.util

import kotlinx.serialization.Serializable

@Serializable
data class JoinedTopicJoinRequestRequest(
    val joinedTopicName: String,
    val sourceTopicName: String
)