package de.faweizz.poc.util

import kotlinx.serialization.Serializable

@Serializable
data class ResourceSharingRequest(
    val id: String,
    val resourceId: String,
    val requestingActorId: String,
    val transformationSteps: List<PendingTransformationStep>,
    val actorsToAccept: Set<String>,
    val acceptedActors: List<String>,
    val declinedActors: List<String>
)
