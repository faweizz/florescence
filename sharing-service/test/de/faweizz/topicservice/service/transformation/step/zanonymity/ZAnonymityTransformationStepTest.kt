package de.faweizz.topicservice.service.transformation.step.zanonymity

import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericRecordBuilder
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ZAnonymityTransformationStepTest {

    private lateinit var zAnonymityTransformationStep: ZAnonymityTransformationStep

    @Before
    fun setUp() {
        zAnonymityTransformationStep = ZAnonymityTransformationStep(
            mapOf(
                ZAnonymityTransformationStep.DELTA_PARAMETER_NAME to DELTA_MILLIS_VALUE,
                ZAnonymityTransformationStep.Z_PARAMETER_NAME to Z_VALUE,
                ZAnonymityTransformationStep.PERSON_ID_COLUMN_PARAMETER_NAME to PERSON_ID_COLUMN_NAME_VALUE
            )
        )
    }

    @Test
    fun `data should fallback to appropriate default values`() {
        val schema = SchemaBuilder.builder()
            .record("person")
            .fields()
            .name("id")
            .type()
            .nullable()
            .stringType()
            .noDefault()
            .name("name")
            .type()
            .nullable()
            .stringType()
            .noDefault()
            .name("birth_year")
            .type()
            .nullable()
            .intType()
            .noDefault()
            .endRecord()

        val firstMessage = GenericRecordBuilder(schema)
            .set("id", "first-person")
            .set("name", "Max")
            .set("birth_year", 1996)
            .build()

        val secondMessage = GenericRecordBuilder(schema)
            .set("id", "second-person")
            .set("name", "Peter")
            .set("birth_year", 1996)
            .build()

        val thirdMessage = GenericRecordBuilder(schema)
            .set("id", "third-person")
            .set("name", "Peter")
            .set("birth_year", 1997)
            .build()

        val fourthMessage = GenericRecordBuilder(schema)
            .set("id", "fourth-person")
            .set("name", "Peter")
            .set("birth_year", 1996)
            .build()

        val fifthMessage = GenericRecordBuilder(schema)
            .set("id", "fourth-person")
            .set("name", "Peter")
            .set("birth_year", 1998)
            .build()

        val expectedTransformedFirstMessage = GenericRecordBuilder(schema)
            .set("id", null)
            .set("name", null)
            .set("birth_year", null)
            .build()
        val actualTransformedFirstMessage = zAnonymityTransformationStep.apply(0, firstMessage)
        assertEquals(expectedTransformedFirstMessage, actualTransformedFirstMessage)

        val expectedTransformedSecondMessage = GenericRecordBuilder(schema)
            .set("id", null)
            .set("name", null)
            .set("birth_year", null)
            .build()
        val actualTransformedSecondMessage = zAnonymityTransformationStep.apply(100, secondMessage)
        assertEquals(expectedTransformedSecondMessage, actualTransformedSecondMessage)

        val expectedTransformedThirdMessage = GenericRecordBuilder(schema)
            .set("id", null)
            .set("name", null)
            .set("birth_year", null)
            .build()
        val actualTransformedThirdMessage = zAnonymityTransformationStep.apply(200, thirdMessage)
        assertEquals(expectedTransformedThirdMessage, actualTransformedThirdMessage)

        val expectedTransformedFourthMessage = GenericRecordBuilder(schema)
            .set("id", null)
            .set("name", "Peter")
            .set("birth_year", 1996)
            .build()
        val actualTransformedFourthMessage = zAnonymityTransformationStep.apply(300, fourthMessage)
        assertEquals(expectedTransformedFourthMessage, actualTransformedFourthMessage)

        val expectedTransformedFifthMessage = GenericRecordBuilder(schema)
            .set("id", null)
            .set("name", "Peter")
            .set("birth_year", null)
            .build()
        val actualTransformedFifthMessage = zAnonymityTransformationStep.apply(400, fifthMessage)
        assertEquals(expectedTransformedFifthMessage, actualTransformedFifthMessage)
    }

    companion object {
        private const val Z_VALUE = "3"
        private const val DELTA_MILLIS_VALUE = "10000"
        private const val PERSON_ID_COLUMN_NAME_VALUE = "id"
    }
}