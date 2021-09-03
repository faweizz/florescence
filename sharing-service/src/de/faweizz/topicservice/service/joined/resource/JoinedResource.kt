package de.faweizz.topicservice.service.joined.resource

data class JoinedResource(
    val resourceId: String,
    val owningActorIds: MutableList<String>,
    val codeExecutions: MutableList<CodeExecution> = mutableListOf(),
    val schema: String,
    val description: String
)