package de.faweizz.poc.util

import kotlinx.serialization.Serializable

@Serializable
data class CodeExecutionPoll(
    val id: String,
    val requester: String,
    val resourceName: String,
    val name: String,
    val gitUrl: String,
    val commitHash: String,
    val comment: String,
    val actorsToAccept: Set<String>,
    val acceptedActors: MutableSet<String>,
    val declinedActors: MutableSet<String>
)
