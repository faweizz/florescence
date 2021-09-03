package de.faweizz.topicservice.service.transformation.step.watermark.detection

import de.faweizz.topicservice.service.transformation.step.watermark.calculateNumberOfLeastSignificantBits
import de.faweizz.topicservice.service.transformation.step.watermark.chanceOf
import de.faweizz.topicservice.service.transformation.step.watermark.getByteOf
import de.faweizz.topicservice.service.transformation.step.watermark.pickOneOf
import org.apache.avro.generic.GenericRecord
import java.security.SecureRandom
import kotlin.experimental.and

class WaterMarkDetector(
    private val key: String,
    private val primaryKeyColumnName: String,
    private val markingRatio: Double,
    private val columnsNamesOfColumnsAvailableForMarking: List<String>
) {

    fun detect(genericRecords: List<GenericRecord>): DetectionResult {
        var totalCount = 0
        var matchCount = 0

        genericRecords.forEach { genericRecord ->
            val seed = genericRecord[primaryKeyColumnName].toString() + key

            val randomGenerator = SecureRandom.getInstance("SHA1PRNG", "SUN")
            randomGenerator.setSeed(seed.toByteArray())

            if (randomGenerator.chanceOf(markingRatio)) {
                val attributeToMark = randomGenerator.pickOneOf(columnsNamesOfColumnsAvailableForMarking)
                val typeOfAttributeToMark = genericRecord.schema.getField(attributeToMark).schema()

                val numberOfLeastSignificantBits =
                    calculateNumberOfLeastSignificantBits(attributeToMark, typeOfAttributeToMark) ?: return@forEach
                if (numberOfLeastSignificantBits == 0) return@forEach
                val byteToSwitch = randomGenerator.pickOneOf((0 until numberOfLeastSignificantBits).toList())

                val shouldBeOne = byteToSwitch % 2 == 1
                val value =
                    getByteOf(byteToSwitch, genericRecord[attributeToMark], typeOfAttributeToMark) ?: return@forEach

                val expectedAndResult = if (shouldBeOne) 1.toByte() else 0.toByte()
                val valueOfLastBit = value and 1

                if (valueOfLastBit == expectedAndResult) {
                    matchCount++
                }
                totalCount++
            }
        }

        return DetectionResult(totalCount, matchCount)
    }
}