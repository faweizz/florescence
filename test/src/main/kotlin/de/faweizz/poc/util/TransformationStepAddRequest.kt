package de.faweizz.poc.util

import kotlinx.serialization.Serializable

@Serializable
data class TransformationStepAddRequest(
    val transformationStepId: String,
    val data: Map<String, String>
)