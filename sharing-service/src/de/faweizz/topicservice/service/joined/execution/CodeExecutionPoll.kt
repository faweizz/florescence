package de.faweizz.topicservice.service.joined.execution

import de.faweizz.topicservice.service.polling.Pollable
import java.util.*

data class CodeExecutionPoll(
    override val id: String = UUID.randomUUID().toString(),
    val requester: String,
    val resourceName: String,
    val name: String,
    val gitUrl: String,
    val commitHash: String,
    val comment: String,
    override val actorsToAccept: Set<String> = emptySet(),
    override val acceptedActors: MutableSet<String> = mutableSetOf(),
    override val declinedActors: MutableSet<String> = mutableSetOf()
) : Pollable