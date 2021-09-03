package de.faweizz.poc.showcase

import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

data class Vector(
    val x: Double,
    val y: Double
) {
    fun calculateMagnitude(): Double {
        return sqrt(x.pow(2) + y.pow(2))
    }

    companion object {
        fun generate(): Vector {
            val random = Random()
            return Vector(random.nextDouble(), random.nextDouble())
        }
    }
}


