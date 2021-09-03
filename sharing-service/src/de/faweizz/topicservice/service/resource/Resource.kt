package de.faweizz.topicservice.service.resource

import de.faweizz.topicservice.Actor

data class Resource(
    val id: String,
    val owningActors: List<String>,
    val ownerAccessLevel: AccessLevel,
    val schema: String,
    val description: String
) {
    fun isOwner(actor: Actor) = owningActors.contains(actor.name)

    fun hasOwnerAccess(actor: Actor, permissionLevel: AccessLevel): Boolean {
        return isOwner(actor) && ownerAccessLevel isHigherOrEqualThan permissionLevel
    }
}
