package de.faweizz.topicservice.service.transformation.step

import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

abstract class TransformationStep(
    internal val data: Map<String, String>
) {

    abstract val requiredDataKeys: List<String>

    fun validate() {
        requiredDataKeys.forEach {
            if (!data.containsKey(it)) throw Exception("Key $it is missing in configuration")
        }
    }

    abstract fun calculateMutatedSchema(schema: Schema): Schema

    abstract fun apply(timeMillis: Long, genericRecord: GenericRecord): GenericRecord
}