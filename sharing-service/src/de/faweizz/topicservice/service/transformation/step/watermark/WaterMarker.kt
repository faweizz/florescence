package de.faweizz.topicservice.service.transformation.step.watermark

import org.apache.avro.generic.GenericRecord
import java.security.SecureRandom

class WaterMarker(
    private val key: String,
    private val primaryKeyColumnName: String,
    private val markingRatio: Double,
    private val columnsNamesOfColumnsAvailableForMarking: List<String>
) {

    fun waterMark(genericRecord: GenericRecord) {
        val seed = genericRecord[primaryKeyColumnName].toString() + key

        val randomGenerator = SecureRandom.getInstance("SHA1PRNG", "SUN")
        randomGenerator.setSeed(seed.toByteArray())

        if (randomGenerator.chanceOf(markingRatio)) {
            val attributeToMark = randomGenerator.pickOneOf(columnsNamesOfColumnsAvailableForMarking)
            val typeOfAttributeToMark = genericRecord.schema.getField(attributeToMark).schema()

            val numberOfLeastSignificantBits =
                calculateNumberOfLeastSignificantBits(attributeToMark, typeOfAttributeToMark) ?: return
            if (numberOfLeastSignificantBits == 0) return
            val bitToSwitch = randomGenerator.pickOneOf((0 until numberOfLeastSignificantBits).toList())

            val newValue = switchLeastSignificantBit(
                switchToOne = bitToSwitch % 2 == 1,
                byteIndexToMark = bitToSwitch,
                attributeToMark = genericRecord[attributeToMark],
                typeOfAttributeToMark = typeOfAttributeToMark
            )

            genericRecord.put(attributeToMark, newValue)
        }
    }
}

fun <T> SecureRandom.pickOneOf(list: List<T>): T = list[(nextDouble() * list.size).toInt()]
fun SecureRandom.chanceOf(chance: Double): Boolean = nextDouble() <= chance