package de.faweizz.poc.util

import kotlinx.serialization.Serializable

@Serializable
data class PendingTransformationStep(
    val id: String,
    val step: TransformationStepData
)
