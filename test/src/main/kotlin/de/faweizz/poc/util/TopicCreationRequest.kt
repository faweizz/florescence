package de.faweizz.poc.util

import kotlinx.serialization.Serializable

@Serializable
data class TopicCreationRequest(
    val topicName: String,
    val schema: String,
    val description: String
)