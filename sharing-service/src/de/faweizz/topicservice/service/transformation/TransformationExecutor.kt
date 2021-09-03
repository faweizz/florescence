package de.faweizz.topicservice.service.transformation

import de.faweizz.topicservice.service.transformation.step.TransformationStepFactory
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

class TransformationExecutor(
    transformationStepData: List<TransformationStepData>
) {

    private val transformationSteps = transformationStepData.map {
        TransformationStepFactory().instantiate(it)
    }

    fun calculateSchema(schema: Schema): Schema {
        var mutatingSchema = schema

        transformationSteps.forEach {
            mutatingSchema = it.calculateMutatedSchema(mutatingSchema)
        }

        return mutatingSchema
    }

    fun apply(genericRecord: GenericRecord): GenericRecord {
        val time = System.currentTimeMillis()
        var mutatingRecord = genericRecord

        transformationSteps.forEach {
            mutatingRecord = it.apply(time, mutatingRecord)
        }

        return mutatingRecord
    }
}