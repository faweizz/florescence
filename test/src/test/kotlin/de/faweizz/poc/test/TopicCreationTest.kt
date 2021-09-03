package de.faweizz.poc.test

import de.faweizz.poc.util.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*
import kotlin.test.assertEquals

class TopicCreationTest {

    private lateinit var testMessage: GenericRecord
    private lateinit var schema: Schema

    @BeforeEach
    internal fun setUp() {
        schema = SchemaBuilder.builder()
            .record("test_schema")
            .fields()
            .name("id").type().stringType().noDefault()
            .name("amount").type().intType().noDefault()
            .endRecord()

        testMessage = GenericRecordBuilder(schema)
            .set("id", UUID.randomUUID().toString())
            .set("amount", 100)
            .build()
    }

    @Test
    fun `actor should be able to create topic`() {
        runBlocking {
            val accessToken = getJwt(USERNAME, SECRET, "http://keycloak:8080")

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
    fun `actor should be able to create client for topic`() {
        runBlocking {
            val accessToken = getJwt(USERNAME, SECRET, "http://keycloak:8080")

            val fullTopicName = "$USERNAME.$TOPIC_NAME"

            createHttpClient().use {
                val clientInfo = it.post<ClientInfo>("http://localhost:8081/clients") {
                    header("Authorization", "Bearer ${accessToken.accessToken}")
                    header("Content-Type", "application/json")
                    body = ClientCreationRequest(
                        name = CLIENT_NAME + "11",
                        inputResourceIds = emptyList(),
                        outputResourceIds = listOf(fullTopicName)
                    )
                }

                val consumer = createTestKafkaConsumer(clientInfo.consumerGroup, clientInfo.name, clientInfo.secret)
                consumer.subscribe(listOf(fullTopicName))
                consumer.poll(Duration.ofMillis(1000))

                val producer = createTestKafkaProducer(clientInfo.consumerGroup, clientInfo.name, clientInfo.secret)
                producer.send(
                    ProducerRecord(fullTopicName, testMessage.serialize(schema))
                ) { metadata, exception -> println("$metadata, $exception") }

                val messages = consumer.poll(Duration.ofMillis(1000))

                assertEquals(1, messages.count())
                assertEquals(testMessage, messages.first().value().deserialize(schema))
            }
        }
    }

    @Test
    fun `actor should be able to request sharing of topic`() {
        runBlocking {
            val requesterAccessToken = getJwt(SECOND_USER, SECOND_USER_SECRET, KEYCLOAK_ADDRESS)
            val resourceOwnerAccessToken = getJwt(USERNAME, SECRET, KEYCLOAK_ADDRESS)

            val resourceToRequest = "$USERNAME.$TOPIC_NAME"

            createHttpClient().use {
                val request =
                    it.post<ResourceSharingRequest>("http://localhost:8081/resource/$resourceToRequest/request-sharing") {
                        header("Authorization", "Bearer ${requesterAccessToken.accessToken}")
                    }

                it.post<HttpResponse>("http://localhost:8081/sharing-request/${request.id}/accept") {
                    header("Authorization", "Bearer ${resourceOwnerAccessToken.accessToken}")
                }

                val requesterClient = it.post<ClientInfo>("http://localhost:8081/clients") {
                    header("Authorization", "Bearer ${requesterAccessToken.accessToken}")
                    header("Content-Type", "application/json")
                    body = ClientCreationRequest(
                        name = "resource-requester-test-client",
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
                        name = "resource-creator-test-client",
                        inputResourceIds = emptyList(),
                        outputResourceIds = listOf(resourceToRequest)
                    )
                }

                val producer = createTestKafkaProducer(
                    resourceOwnerClient.consumerGroup,
                    resourceOwnerClient.name,
                    resourceOwnerClient.secret
                )
                producer.send(ProducerRecord(resourceToRequest, testMessage.serialize(schema)))

                val result = consumer.poll(Duration.ofMillis(5000))

                assertEquals(1, result.count())
                assertEquals(testMessage, result.first().value().deserialize(schema))
            }
        }
    }

    companion object {
        private const val CLIENT_NAME = "test-client1011111121"
        private const val TOPIC_NAME = "test-topic312111113"
        private const val USERNAME = "alice"
        private const val SECRET = "alice-secret"
        private const val SECOND_USER = "bob"
        private const val SECOND_USER_SECRET = "bob-secret"
        private const val KEYCLOAK_ADDRESS = "http://keycloak:8080"
    }
}