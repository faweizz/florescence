package de.faweizz.poc.util

import kotlinx.serialization.Serializable

@Serializable
data class JoinedResource(
    val resourceId: String,
    val owningActorIds: MutableList<String>,
    val codeExecutions: MutableList<CodeExecution> = mutableListOf(),
    val schema: String,
    val description: String
)