package de.faweizz.poc.test

import de.faweizz.poc.showcase.Vector
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs

class PiApproximation {

    @Test
    fun approximatePi() {
        var vectorsChecked = 0
        var vectorsInUnitCircle = 0

        repeat(1000) {
            Thread.sleep(100)
            val random = java.util.Random()
            val vector = Vector(random.nextDouble(), random.nextDouble())
            if (vector.calculateMagnitude() <= 1) vectorsInUnitCircle++
            vectorsChecked++
            val currentPi = (vectorsInUnitCircle.toDouble() / vectorsChecked.toDouble()) * 4.0
            println("Pi is probably $currentPi (diff ${abs(PI - currentPi)})")
        }
    }
}