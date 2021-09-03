package de.faweizz.topicservice.service.joined.execution

data class CodeExecutionRequest(
    val resourceName: String,
    val gitUrl: String,
    val commitHash: String,
    val name: String,
    val comment: String
)