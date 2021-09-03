package de.faweizz.poc.util

import kotlinx.serialization.Serializable

@Serializable
data class CodeExecutionRequest(
    val resourceName: String,
    val gitUrl: String,
    val commitHash: String,
    val name: String,
    val comment: String
)
