package de.faweizz.topicservice.service.transformation.step.watermark

import de.faweizz.topicservice.service.transformation.step.TransformationStep
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

/**
 * Implementation of Algorithm proposed in https://link.springer.com/article/10.1007/s00778-003-0097-x
 */

class WatermarkTransformationStep(
    data: Map<String, String>
) : TransformationStep(data) {

    private val waterMarker = WaterMarker(
        key = data["key"] ?: throw Exception("missing key"),
        columnsNamesOfColumnsAvailableForMarking = data["columnsNamesOfColumnsAvailableForMarking"]?.split(",")
            ?: throw Exception("columnsNamesOfColumnsAvailableForMarking missing"),
        markingRatio = data["markingRatio"]?.toDoubleOrNull() ?: throw Exception("markingRatio parameter missing"),
        primaryKeyColumnName = data["primaryKeyColumnName"] ?: throw Exception("missing primaryKeyColumnName parameter")
    )

    override val requiredDataKeys: List<String> = listOf()

    override fun calculateMutatedSchema(schema: Schema): Schema {
        return schema
    }

    override fun apply(timeMillis: Long, genericRecord: GenericRecord): GenericRecord {
        waterMarker.waterMark(genericRecord)
        return genericRecord
    }
}