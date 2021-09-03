package de.faweizz.topicservice.service.resource

enum class AccessLevel(private val level: Int) {
    NONE(0), READ(1), WRITE(2);

    infix fun isHigherOrEqualThan(other: AccessLevel): Boolean {
        return level >= other.level
    }
}
