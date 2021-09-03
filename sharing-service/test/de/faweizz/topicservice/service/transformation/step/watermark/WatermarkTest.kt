package de.faweizz.topicservice.service.transformation.step.watermark

import de.faweizz.topicservice.service.transformation.step.watermark.detection.DetectionResult
import de.faweizz.topicservice.service.transformation.step.watermark.detection.WaterMarkDetector
import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericRecordBuilder
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class WatermarkTest {
    private lateinit var waterMarker: WaterMarker
    private lateinit var schema: Schema

    @Before
    fun setUp() {
        schema = SchemaBuilder.builder()
            .record("schema_name")
            .fields()
            .nullableString("id", "")
            .nullableInt("int", -1)
            .nullableDouble("double", 0.0)
            .nullableString("string", "")
            .endRecord()

        waterMarker = WaterMarker(
            key = KEY,
            primaryKeyColumnName = "id",
            markingRatio = 0.6,
            columnsNamesOfColumnsAvailableForMarking = listOf("int", "double", "string")
        )
    }

    @Test
    fun detect() {
        val data = (0..10).map {
            GenericRecordBuilder(schema)
                .set("id", it)
                .set("int", it * 100)
                .set("double", it * 0.211)
                .set("string", "$it - string")
                .build()
        }
        data.forEach { waterMarker.waterMark(it) }

        val waterMarkDetector = WaterMarkDetector(
            key = KEY,
            primaryKeyColumnName = "id",
            markingRatio = 0.6,
            columnsNamesOfColumnsAvailableForMarking = listOf("int", "double", "string")
        )

        assertEquals(DetectionResult(8, 8), waterMarkDetector.detect(data))
    }

    companion object {
        private const val KEY = "KEY 1 2 3"
    }
}