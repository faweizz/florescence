package de.faweizz.topicservice.service.joined.resource

import de.faweizz.topicservice.service.polling.Pollable
import java.util.*

data class JoinedResourceJoinRequest(
    override val id: String = UUID.randomUUID().toString(),
    val sourcedResource: String,
    val targetJoinedResource: String,
    val requestingActor: String,
    override val actorsToAccept: Set<String>,
    override val acceptedActors: MutableSet<String> = mutableSetOf(),
    override val declinedActors: MutableSet<String> = mutableSetOf()
) : Pollable
