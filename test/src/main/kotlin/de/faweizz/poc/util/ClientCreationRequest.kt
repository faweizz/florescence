package de.faweizz.poc.util

import kotlinx.serialization.Serializable

@Serializable
data class ClientCreationRequest(
    val name: String,
    val inputResourceIds: List<String>,
    val outputResourceIds: List<String>
)