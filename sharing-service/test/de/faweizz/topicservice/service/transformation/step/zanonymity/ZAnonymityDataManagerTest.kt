package de.faweizz.topicservice.service.transformation.step.zanonymity

import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZAnonymityDataManagerTest {

    private lateinit var zAnonymityDataManager: ZAnonymityDataManager

    @Before
    fun setUp() {
        zAnonymityDataManager = ZAnonymityDataManager(Z_VALUE, DELTA_MILLIS_VALUE)
    }

    @Test
    fun `a single value should not be z anonymous`() {
        val value = 1
        val columnName = "age"

        assertFalse(zAnonymityDataManager.isFieldZAnonymous(0, "first-person", columnName, value))
    }

    @Test
    fun `the third value within ten seconds should be z anonymous`() {
        val value = 1
        val columnName = "age"

        assertFalse(zAnonymityDataManager.isFieldZAnonymous(0, "first-person", columnName, value))
        assertFalse(zAnonymityDataManager.isFieldZAnonymous(1000, "second-person", columnName, value))
        assertTrue(zAnonymityDataManager.isFieldZAnonymous(9000, "third-person", columnName, value))
    }

    @Test
    fun `the third value after ten seconds should not be z anonymous`() {
        val value = 1
        val columnName = "age"

        assertFalse(zAnonymityDataManager.isFieldZAnonymous(0, "first-person", columnName, value))
        assertFalse(zAnonymityDataManager.isFieldZAnonymous(1000, "second-person", columnName, value))
        assertFalse(zAnonymityDataManager.isFieldZAnonymous(11000, "third-person", columnName, value))
    }

    @Test
    fun `the third value should not be z anonymous when another value from another column is given`() {
        val value = 1
        val columnName = "age"
        val secondColumnName = "name"

        assertFalse(zAnonymityDataManager.isFieldZAnonymous(0, "first-person", columnName, value))
        assertFalse(zAnonymityDataManager.isFieldZAnonymous(1000, "second-person", secondColumnName, value))
        assertFalse(zAnonymityDataManager.isFieldZAnonymous(9000, "third-person", columnName, value))
    }

    @Test
    fun `the third value should not be z anonymous when the columns equal but the values differ`() {
        val value = 1
        val secondValue = 2
        val columnName = "age"

        assertFalse(zAnonymityDataManager.isFieldZAnonymous(0, "first-person", columnName, value))
        assertFalse(zAnonymityDataManager.isFieldZAnonymous(1000, "second-person", columnName, secondValue))
        assertFalse(zAnonymityDataManager.isFieldZAnonymous(9000, "third-person", columnName, value))
    }

    companion object {
        private const val Z_VALUE = 3
        private const val DELTA_MILLIS_VALUE = 10000L
    }
}