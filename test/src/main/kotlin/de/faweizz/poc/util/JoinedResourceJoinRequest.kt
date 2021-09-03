package de.faweizz.poc.util

import kotlinx.serialization.Serializable

@Serializable
data class JoinedResourceJoinRequest(
    val id: String,
    val sourcedResource: String,
    val targetJoinedResource: String,
    val requestingActor: String,
    val actorsToAccept: Set<String>,
    val acceptedActors: Set<String>,
    val declinedActors: Set<String>
)
