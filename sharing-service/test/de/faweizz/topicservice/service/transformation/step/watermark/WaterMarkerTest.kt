package de.faweizz.topicservice.service.transformation.step.watermark

import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class WaterMarkerTest {

    private lateinit var waterMarker: WaterMarker
    private lateinit var record: GenericRecord

    @Before
    fun setUp() {
        val schema = SchemaBuilder.builder()
            .record("schema_name")
            .fields()
            .nullableString("id", "")
            .nullableInt("int", -1)
            .nullableDouble("double", 0.0)
            .nullableString("string", "")
            .endRecord()

        record = GenericRecordBuilder(schema)
            .set("id", "id value")
            .set("int", 123412343124)
            .set("double", 1.13412)
            .set("string", "some string")
            .build()

        waterMarker = WaterMarker(
            key = KEY,
            primaryKeyColumnName = "id",
            markingRatio = 0.5,
            columnsNamesOfColumnsAvailableForMarking = listOf("int", "double", "string")
        )
    }

    @Test
    fun watermark() {
        waterMarker.waterMark(record)

        assertEquals("rome string", record["string"])
        assertEquals(123412343124, record["int"])
        assertEquals(1.13412, record["double"])
    }

    companion object {
        private const val KEY = "KEY 1 2"
    }
}