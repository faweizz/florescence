package de.faweizz.poc.test

import de.faweizz.poc.util.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals

class ZAnonymityTransformationTest {

    private lateinit var schema: Schema

    @BeforeEach
    internal fun setUp() {
        schema = SchemaBuilder.builder()
            .record("test_schema")
            .fields()
            .nullableString("id", "")
            .nullableInt("birth_year", -1)
            .nullableInt("amount", -1)
            .endRecord()
    }

    @Test
    fun `actor should be able to create topic`() {
        runBlocking {
            val accessToken = getJwt(USERNAME, SECRET, KEYCLOAK_ADDRESS)

            createHttpClient().use {
                val response = it.post<HttpResponse>("http://localhost:8081/resource") {
                    header("Authorization", "Bearer ${accessToken.accessToken}")
                    header("Content-Type", "application/json")
                    body = TopicCreationRequest(TOPIC_NAME, schema.toString(), "some description")
                }

                assertEquals(HttpStatusCode.OK, response.status)
            }
        }
    }

    @Test
    fun `transformation steps should be added when requesting sharing`() {
        runBlocking {
            val requesterAccessToken = getJwt(SECOND_USER, SECOND_USER_SECRET, KEYCLOAK_ADDRESS)
            val resourceOwnerAccessToken = getJwt(USERNAME, SECRET, KEYCLOAK_ADDRESS)

            val resourceToRequest = "$USERNAME.$TOPIC_NAME"

            createHttpClient().use {
                val request =
                    it.post<ResourceSharingRequest>("http://localhost:8081/resource/$resourceToRequest/request-sharing") {
                        header("Authorization", "Bearer ${requesterAccessToken.accessToken}")
                    }

                it.post<HttpResponse>("http://localhost:8081/sharing-request/${request.id}/transformation") {
                    header("Authorization", "Bearer ${resourceOwnerAccessToken.accessToken}")
                    header("Content-Type", "application/json")
                    body = TransformationStepAddRequest(
                        transformationStepId = "de.faweizz.topicservice.service.transformation.step.zanonymity.ZAnonymityTransformationStep",
                        data = mapOf(
                            "z" to "3",
                            "delta" to "10000",
                            "personIdColumn" to "id"
                        )
                    )
                }

                it.post<HttpResponse>("http://localhost:8081/sharing-request/${request.id}/accept") {
                    header("Authorization", "Bearer ${resourceOwnerAccessToken.accessToken}")
                }
            }
        }
    }

    @Test
    fun `transformation should produce correct data`() {
        runBlocking {
            val requesterAccessToken = getJwt(SECOND_USER, SECOND_USER_SECRET, KEYCLOAK_ADDRESS)
            val resourceOwnerAccessToken = getJwt(USERNAME, SECRET, KEYCLOAK_ADDRESS)

            val sourceResource = "$USERNAME.$TOPIC_NAME"
            val resourceToRequest = "$SECOND_USER.mirror.$USERNAME.$TOPIC_NAME"

            createHttpClient().use {
                val requesterClient = it.post<ClientInfo>("http://localhost:8081/clients") {
                    header("Authorization", "Bearer ${requesterAccessToken.accessToken}")
                    header("Content-Type", "application/json")
                    body = ClientCreationRequest(
                        name = "anonymity-consumer14",
                        inputResourceIds = listOf(resourceToRequest),
                        outputResourceIds = emptyList()
                    )
                }

                val consumer =
                    createTestKafkaConsumer(requesterClient.consumerGroup, requesterClient.name, requesterClient.secret)
                consumer.subscribe(listOf(resourceToRequest))
                consumer.poll(Duration.ofMillis(1000))

                val resourceOwnerClient = it.post<ClientInfo>("http://localhost:8081/clients") {
                    header("Authorization", "Bearer ${resourceOwnerAccessToken.accessToken}")
                    header("Content-Type", "application/json")
                    body = ClientCreationRequest(
                        name = "anonymity-producer14",
                        inputResourceIds = emptyList(),
                        outputResourceIds = listOf(sourceResource)
                    )
                }

                val producer = createTestKafkaProducer(
                    resourceOwnerClient.consumerGroup,
                    resourceOwnerClient.name,
                    resourceOwnerClient.secret
                )

                (0..3).forEach { index ->
                    val message = GenericRecordBuilder(schema)
                        .set("id", index.toString())
                        .set("birth_year", 1999)
                        .set("amount", 12)
                        .build()

                    producer.send(ProducerRecord(sourceResource, message.serialize(schema)))
                }

                val result = consumer.poll(Duration.ofMillis(10000))

                assertEquals(4, result.count())

                val messages = result.map { message -> message.value().deserialize(schema) }

                assertEquals(
                    GenericRecordBuilder(schema)
                        .set("id", "")
                        .set("birth_year", -1)
                        .set("amount", -1)
                        .build(),
                    messages[0]
                )

                assertEquals(
                    GenericRecordBuilder(schema)
                        .set("id", "")
                        .set("birth_year", -1)
                        .set("amount", -1)
                        .build(),
                    messages[1]
                )

                assertEquals(
                    GenericRecordBuilder(schema)
                        .set("id", "")
                        .set("birth_year", 1999)
                        .set("amount", 12)
                        .build(),
                    messages[2]
                )

                assertEquals(
                    GenericRecordBuilder(schema)
                        .set("id", "")
                        .set("birth_year", 1999)
                        .set("amount", 12)
                        .build(),
                    messages[3]
                )
            }
        }
    }

    companion object {
        private const val TOPIC_NAME = "anonymity-source-topic-4"
        private const val USERNAME = "alice"
        private const val SECRET = "alice-secret"
        private const val SECOND_USER = "bob"
        private const val SECOND_USER_SECRET = "bob-secret"
        private const val KEYCLOAK_ADDRESS = "http://keycloak:8080"
    }
}