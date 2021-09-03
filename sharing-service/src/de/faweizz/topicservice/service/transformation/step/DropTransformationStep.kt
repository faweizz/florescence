package de.faweizz.topicservice.service.transformation.step

import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder

class DropTransformationStep(
    data: Map<String, String>
) : TransformationStep(data) {

    override val requiredDataKeys = listOf(COLUMN_TO_DROP_NAME_KEY)

    private fun getColumnToDrop(data: Map<String, String>) = data[COLUMN_TO_DROP_NAME_KEY]
        ?: throw Exception("columnToDrop not supplied")

    override fun calculateMutatedSchema(schema: Schema): Schema {
        val columnToDrop = getColumnToDrop(data)

        val schemaBuilder = SchemaBuilder.builder()
            .record(schema.fullName)
            .fields()

        schema.fields.forEach {
            if (it.name() != columnToDrop) {
                schemaBuilder.name(it.name())
                    .type(it.schema().type.getName())
                    .noDefault()
            }
        }

        return schemaBuilder.endRecord()
    }

    override fun apply(timeMillis: Long, genericRecord: GenericRecord): GenericRecord {
        val columnToDrop = getColumnToDrop(data)
        val mutatedSchema = calculateMutatedSchema(genericRecord.schema)
        val recordBuilder = GenericRecordBuilder(mutatedSchema)

        genericRecord.schema.fields.forEach {
            val fieldName = it.name()
            if (fieldName != columnToDrop) {
                recordBuilder.set(fieldName, genericRecord.get(fieldName))
            }
        }

        return recordBuilder.build()
    }

    companion object {
        private const val COLUMN_TO_DROP_NAME_KEY = "columnToDrop"
    }
}