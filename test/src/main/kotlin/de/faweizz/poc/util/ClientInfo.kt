package de.faweizz.poc.util

import kotlinx.serialization.Serializable

@Serializable
data class ClientInfo(
    val name: String,
    val secret: String,
    val consumerGroup: String
)
