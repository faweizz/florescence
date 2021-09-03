package de.faweizz.topicservice.service.transformation.step.watermark

import org.apache.avro.SchemaBuilder
import org.junit.Test
import kotlin.test.assertEquals

class BitsKtTest {

    @Test
    fun name() {
        val schema = SchemaBuilder.builder()
            .nullable()
            .stringType()

        calculateNumberOfLeastSignificantBits("", schema)
    }

    @Test
    fun `mark string`() {
        val string = "test string haha"
        val result = maskString(string, string.toByteArray().size - 1, false)
        assertEquals("test string hah`", result)
    }

    @Test
    fun `mark int`() {
        val int = 1001123412
        val result = maskInt(int, Int.SIZE_BYTES - 1, true)
        assertEquals(1001123413, result)
    }

    @Test
    fun `mark long`() {
        val long = 1412342134141234123L
        val result = maskLong(long, 1, false)
        assertEquals(1412060659164523467, result)
    }

    @Test
    fun `mark double`() {
        val double = 1.12341234
        val result = maskDouble(double, Double.SIZE_BYTES - 1, true)
        assertEquals(1.12341234, result)
    }

    @Test
    fun `mark float`() {
        val float = 1.12341234F
        val result = maskFloat(float, Float.SIZE_BYTES - 1, true)
        assertEquals(1.1234125F, result)
    }

    @Test
    fun `mark bytes`() {
        val bytes = arrayOf(1.toByte(), 2.toByte(), 3.toByte()).toByteArray()
        val result = maskByte(bytes, bytes.size - 1, false)
        assertEquals(arrayOf(1.toByte(), 2.toByte(), 2.toByte()).toByteArray().toList(), result.toList())
    }
}