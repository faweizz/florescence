package de.faweizz.topicservice.service.model

import de.faweizz.topicservice.service.polling.Pollable
import de.faweizz.topicservice.service.sharing.PendingTransformationStep
import java.util.*

data class ResourceSharingRequest(
    override val id: String = UUID.randomUUID().toString(),
    val resourceId: String,
    val requestingActorId: String,
    var transformationSteps: List<PendingTransformationStep> = emptyList(),
    override val actorsToAccept: Set<String>,
    override val acceptedActors: MutableSet<String> = mutableSetOf(),
    override val declinedActors: MutableSet<String> = mutableSetOf()
) : Pollable