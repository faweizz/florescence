package de.faweizz.topicservice.service.transformation.step.watermark

import org.apache.avro.Schema
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.Charset
import kotlin.experimental.and
import kotlin.experimental.or

fun calculateNumberOfLeastSignificantBits(
    attributeToMark: Any,
    typeOfAttributeToMark: Schema
): Int? {
    return when (getActualType(typeOfAttributeToMark)) {
        Schema.Type.STRING -> (attributeToMark as String).toByteArray().size
        Schema.Type.BYTES -> (attributeToMark as ByteArray).size
        Schema.Type.INT -> Int.SIZE_BYTES
        Schema.Type.LONG -> Long.SIZE_BYTES
        Schema.Type.FLOAT -> Float.SIZE_BYTES
        Schema.Type.DOUBLE -> Double.SIZE_BYTES
        Schema.Type.BOOLEAN -> 1
        else -> null
    }
}

fun switchLeastSignificantBit(
    switchToOne: Boolean,
    byteIndexToMark: Int,
    attributeToMark: Any,
    typeOfAttributeToMark: Schema
): Any? {
    return when (getActualType(typeOfAttributeToMark)) {
        Schema.Type.STRING -> maskString(attributeToMark.toString(), byteIndexToMark, switchToOne)
        Schema.Type.INT -> maskInt((attributeToMark as Int).toInt(), byteIndexToMark, switchToOne)
        Schema.Type.BOOLEAN -> switchToOne
        Schema.Type.FLOAT -> maskFloat(attributeToMark as Float, byteIndexToMark, switchToOne)
        Schema.Type.DOUBLE -> maskDouble(attributeToMark as Double, byteIndexToMark, switchToOne)
        Schema.Type.BYTES -> maskByte(attributeToMark as ByteArray, byteIndexToMark, switchToOne)
        Schema.Type.LONG -> maskLong(attributeToMark as Long, byteIndexToMark, switchToOne)
        else -> null
    }
}

fun getByteOf(byteIndex: Int, attribute: Any, typeOfAttributeToMark: Schema): Byte? {
    return when (getActualType(typeOfAttributeToMark)) {
        Schema.Type.STRING -> attribute.toString().toByteArray()[byteIndex]
        Schema.Type.INT -> ByteBuffer.allocate(Int.SIZE_BYTES).putInt((attribute as Int).toInt()).array()[byteIndex]
        Schema.Type.BOOLEAN -> if(attribute as Boolean) 1 else 0
        Schema.Type.FLOAT -> ByteBuffer.allocate(Float.SIZE_BYTES).putFloat((attribute as Float).toFloat()).array()[byteIndex]
        Schema.Type.DOUBLE -> ByteBuffer.allocate(Double.SIZE_BYTES).putDouble((attribute as Double).toDouble()).array()[byteIndex]
        Schema.Type.BYTES -> (attribute as ByteArray)[byteIndex]
        Schema.Type.LONG -> ByteBuffer.allocate(Long.SIZE_BYTES).putLong((attribute as Long).toLong()).array()[byteIndex]
        else -> null
    }
}

fun maskLong(long: Long, byteIndexToMark: Int, switchToOne: Boolean): Long {
    val bytes = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(long).array()
    bytes[byteIndexToMark] = markByte(bytes[byteIndexToMark], switchToOne)
    return ByteBuffer.allocate(Long.SIZE_BYTES).put(bytes).getLong(0)
}

fun maskByte(bytes: ByteArray, byteIndexToMark: Int, switchToOne: Boolean): ByteArray {
    bytes[byteIndexToMark] = markByte(bytes[byteIndexToMark], switchToOne)
    return bytes
}

fun maskFloat(float: Float, byteIndexToMark: Int, switchToOne: Boolean): Float {
    val bytes = ByteBuffer.allocate(Float.SIZE_BYTES).putFloat(float).array()
    bytes[byteIndexToMark] = markByte(bytes[byteIndexToMark], switchToOne)
    return ByteBuffer.allocate(Float.SIZE_BYTES).put(bytes).getFloat(0)
}

fun maskDouble(double: Double, byteIndexToMark: Int, switchToOne: Boolean): Double {
    val bytes = ByteBuffer.allocate(Double.SIZE_BYTES).putDouble(double).array()
    bytes[byteIndexToMark] = markByte(bytes[byteIndexToMark], switchToOne)
    return ByteBuffer.allocate(Double.SIZE_BYTES).put(bytes).getDouble(0)
}

fun maskInt(int: Int, byteIndexToMark: Int, switchToOne: Boolean): Int {
    val bytes = ByteBuffer.allocate(Int.SIZE_BYTES).putInt(int).array()
    bytes[byteIndexToMark] = markByte(bytes[byteIndexToMark], switchToOne)
    return BigInteger(bytes).toInt()
}

fun maskString(input: String, byteIndexToMark: Int, switchToOne: Boolean): String {
    val stringAsByteArray = input.toByteArray(Charset.defaultCharset())
    stringAsByteArray[byteIndexToMark] = markByte(stringAsByteArray[byteIndexToMark], switchToOne)
    return stringAsByteArray.toString(Charset.defaultCharset())
}

fun markByte(byte: Byte, switchToOne: Boolean): Byte {
    if (switchToOne) {
        return byte or 1.toByte()
    }

    return byte and 1.inv().toByte()
}

private fun getActualType(typeOfAttributeToMark: Schema): Schema.Type {
    if (typeOfAttributeToMark.isUnion && typeOfAttributeToMark.types.size >= 3)
        throw Exception("unsupported type $typeOfAttributeToMark")

    return if (typeOfAttributeToMark.isUnion) typeOfAttributeToMark.types.first { it.type != Schema.Type.NULL }.type
    else typeOfAttributeToMark.type
}