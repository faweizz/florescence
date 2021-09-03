package de.faweizz.topicservice.service.sharing

import de.faweizz.topicservice.service.resource.AccessLevel

data class Transaction(
    val resourceId: String,
    val sharedWithActorId: String?,
    val acceptedByActors: List<String>,
    val transformationId: String?,
    val grantedAccessLevel: AccessLevel = AccessLevel.NONE
)
