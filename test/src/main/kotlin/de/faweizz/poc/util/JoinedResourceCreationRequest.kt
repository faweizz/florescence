package de.faweizz.poc.util

import kotlinx.serialization.Serializable

@Serializable
data class JoinedResourceCreationRequest(
    val name: String,
    val sourceResource: String,
    val description: String
)
