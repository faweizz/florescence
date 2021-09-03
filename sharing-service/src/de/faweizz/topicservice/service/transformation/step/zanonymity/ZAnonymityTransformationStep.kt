/*
Implementation of algorithm proposed in https://ieeexplore.ieee.org/document/9378422
 */

package de.faweizz.topicservice.service.transformation.step.zanonymity

import de.faweizz.topicservice.service.transformation.step.TransformationStep
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

class ZAnonymityTransformationStep(
    data: Map<String, String>
) : TransformationStep(data) {

    override val requiredDataKeys = listOf(Z_PARAMETER_NAME, DELTA_PARAMETER_NAME, PERSON_ID_COLUMN_PARAMETER_NAME)

    private val personIdColumnName =
        data[PERSON_ID_COLUMN_PARAMETER_NAME] ?: throw Exception("person id parameter not given")
    private val zAnonymityDataManager = ZAnonymityDataManager(
        z = data[Z_PARAMETER_NAME]?.toInt() ?: throw Exception("z parameter not given"),
        deltaMillis = data[DELTA_PARAMETER_NAME]?.toLong() ?: throw Exception("delta parameter not given")
    )

    override fun calculateMutatedSchema(schema: Schema): Schema {
        return schema
    }

    override fun apply(timeMillis: Long, genericRecord: GenericRecord): GenericRecord {
        val personId = genericRecord.get(personIdColumnName).toString()

        genericRecord.schema.fields.forEach {
            val fieldName = it.name()
            if (!zAnonymityDataManager.isFieldZAnonymous(
                    timeMillis = timeMillis,
                    personId = personId,
                    attributeName = fieldName,
                    value = genericRecord[fieldName]
                )
            ) {
                genericRecord.put(fieldName, it.defaultVal())
            }
        }

        return genericRecord
    }

    companion object {
        const val Z_PARAMETER_NAME = "z"
        const val DELTA_PARAMETER_NAME = "delta"
        const val PERSON_ID_COLUMN_PARAMETER_NAME = "personIdColumn"
    }
}