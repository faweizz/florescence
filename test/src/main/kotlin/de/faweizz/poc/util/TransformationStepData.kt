package de.faweizz.poc.util

import kotlinx.serialization.Serializable

@Serializable
data class TransformationStepData(
    val transformationStepName: String,
    val data: Map<String, String>
)