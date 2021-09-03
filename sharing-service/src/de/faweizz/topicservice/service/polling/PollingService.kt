package de.faweizz.topicservice.service.polling

import de.faweizz.topicservice.Actor

interface PollingService<T : Pollable> {

    open suspend fun accept(actor: Actor, pollableId: String): T {
        val pollable = getPollableById(pollableId)

        if (!pollable.canAccept(actor.name)) throw Exception("Can't accept poll")
        pollable.accept(actor.name)

        updatePollable(pollable)

        if (pollable.wasAccepted() && !pollable.wasDenied()) onAccepted(pollable)

        return pollable
    }

    open fun decline(actor: Actor, pollableId: String): T {
        val pollable = getPollableById(pollableId)

        if (!pollable.canDecline(actor.name)) throw Exception("Can't decline poll")
        pollable.decline(actor.name)

        updatePollable(pollable)

        return pollable
    }

    fun getPollableById(pollableId: String): T

    fun updatePollable(pollable: T)

    suspend fun onAccepted(pollable: T)
}