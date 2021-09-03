package de.faweizz.topicservice.service.polling

interface Pollable {
    val id: String

    val actorsToAccept: Set<String>
    val acceptedActors: MutableSet<String>
    val declinedActors: MutableSet<String>

    fun canAccept(actor: String): Boolean {
        return actorsToAccept.contains(actor)
                && !acceptedActors.contains(actor)
                && !declinedActors.contains(actor)
    }

    fun canDecline(actor: String): Boolean {
        return actorsToAccept.contains(actor)
                && !acceptedActors.contains(actor)
                && !declinedActors.contains(actor)
    }

    fun wasAccepted(): Boolean {
        return declinedActors.isEmpty() && actorsToAccept == acceptedActors
    }

    fun wasDenied(): Boolean {
        return declinedActors.isNotEmpty()
    }

    fun accept(name: String) {
        acceptedActors += name
    }

    fun decline(name: String) {
        declinedActors += name
    }
}