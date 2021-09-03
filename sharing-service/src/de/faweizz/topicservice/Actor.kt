package de.faweizz.topicservice

import io.ktor.auth.*

data class Actor(
    val name: String,
) : Principal
