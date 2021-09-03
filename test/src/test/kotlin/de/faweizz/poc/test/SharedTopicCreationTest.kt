package de.faweizz.poc.test

import de.faweizz.poc.util.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.avro.Schema
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericRecord
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.*
import kotlin.test.assertEquals

class SharedTopicCreationTest {

    private lateinit var testMessage: GenericRecord
    private lateinit var schema: Schema

    @BeforeEach
    internal fun setUp() {
        schema = SchemaBuilder.builder()
            .record("test_schema_2")
            .fields()
            .name("uid").type().stringType().noDefault()
            .name("someNumber").type().intType().noDefault()
            .endRecord()

        testMessage = GenericRecordBuilder(schema)
            .set("uid", UUID.randomUUID().toString())
            .set("someNumber", 199)
            .build()
    }


    @Test
    fun `actor should create source resource`() {
        runBlocking {
            val accessToken = getJwt(FIRST_USER_NAME, FIRST_USER_PASSWORD, KEYCLOAK_ADDRESS)

            createHttpClient().use {
                val response = it.post<HttpResponse>("http://localhost:8081/resource") {
                    header("Authorization", "Bearer ${accessToken.accessToken}")
                    header("Content-Type", "application/json")
                    body = TopicCreationRequest(ALICE_SOURCE_RESOURCE, schema.toString(), "description")
                }

                assertEquals(HttpStatusCode.OK, response.status)
            }
        }
    }

    @Test
    fun `actor should be able to create joined resource`() {
        runBlocking {
            val accessToken =
                getJwt(FIRST_USER_NAME, FIRST_USER_PASSWORD, KEYCLOAK_ADDRESS)

            createHttpClient().use {
                val createdResource =
                    it.post<JoinedResource>("http://localhost:8081/joined-resource") {
                        header("Authorization", "Bearer ${accessToken.accessToken}")
                        header("Content-Type", "application/json")
                        body = JoinedResourceCreationRequest(
                            name = SHARED_TOPIC_NAME,
                            sourceResource = "alice.$ALICE_SOURCE_RESOURCE",
                            description = "a cool resource"
                        )
                    }

                assertEquals("joined_$SHARED_TOPIC_NAME", createdResource.resourceId)
                assertEquals(listOf("alice"), createdResource.owningActorIds)
                assertEquals(emptyList(), createdResource.codeExecutions)
                assertEquals(schema.toString(), createdResource.schema)
            }
        }
    }

    @Test
    fun `actor should not be able to create client for joined topic`() {
        runBlocking {
            val accessToken = getJwt(FIRST_USER_NAME, FIRST_USER_PASSWORD, KEYCLOAK_ADDRESS)

            val fullTopicName = "joined_$SHARED_TOPIC_NAME"

            createHttpClient().use {
                assertThrows<ServerResponseException> {
                    runBlocking {
                        it.post<HttpResponse>("http://localhost:8081/clients") {
                            header("Authorization", "Bearer ${accessToken.accessToken}")
                            header("Content-Type", "application/json")
                            body = ClientCreationRequest(
                                name = "invalid-client-name",
                                inputResourceIds = listOf(fullTopicName),
                                outputResourceIds = emptyList()
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `second actor should be able to create source resource`() {
        runBlocking {
            val accessToken = getJwt(SECOND_USER, SECOND_USER_PASSWORD, KEYCLOAK_ADDRESS)

            createHttpClient().use {
                val response = it.post<HttpResponse>("http://localhost:8081/resource") {
                    header("Authorization", "Bearer ${accessToken.accessToken}")
                    header("Content-Type", "application/json")
                    body = TopicCreationRequest(BOB_SOURCE_RESOURCE, schema.toString(), "description")
                }

                assertEquals(HttpStatusCode.OK, response.status)
            }
        }
    }

    @Test
    fun `second actor should be able to create joined resource join request and first user should be able to accept it`() {
        runBlocking {
            val accessToken = getJwt(SECOND_USER, SECOND_USER_PASSWORD, KEYCLOAK_ADDRESS)

            createHttpClient().use {
                val sourceTopicName = "bob.$BOB_SOURCE_RESOURCE"
                val joinedTopicName = "joined_$SHARED_TOPIC_NAME"
                val response = it.post<JoinedResourceJoinRequest>("http://localhost:8081/joined-resource-poll") {
                    header("Authorization", "Bearer ${accessToken.accessToken}")
                    header("Content-Type", "application/json")
                    body = JoinedTopicJoinRequestRequest(
                        joinedTopicName,
                        sourceTopicName
                    )
                }

                assertEquals(sourceTopicName, response.sourcedResource)
                assertEquals(joinedTopicName, response.targetJoinedResource)
                assertEquals(SECOND_USER, response.requestingActor)
                assertEquals(setOf(FIRST_USER_NAME), response.actorsToAccept)
                assertEquals(setOf(), response.acceptedActors)
                assertEquals(setOf(), response.declinedActors)

                val aliceAccessToken = getJwt(FIRST_USER_NAME, FIRST_USER_PASSWORD, KEYCLOAK_ADDRESS)

                val acceptResponse =
                    it.post<JoinedResourceJoinRequest>("http://localhost:8081/joined-resource-poll/${response.id}/accept") {
                        header("Authorization", "Bearer ${aliceAccessToken.accessToken}")
                    }

                assertEquals(sourceTopicName, acceptResponse.sourcedResource)
                assertEquals(joinedTopicName, acceptResponse.targetJoinedResource)
                assertEquals(SECOND_USER, acceptResponse.requestingActor)
                assertEquals(setOf(FIRST_USER_NAME), acceptResponse.actorsToAccept)
                assertEquals(setOf(FIRST_USER_NAME), acceptResponse.acceptedActors)
                assertEquals(setOf(), acceptResponse.declinedActors)
            }
        }
    }

    @Test
    fun `first actor should be able to create code execution request and second actor should be able to accept it`() {
        runBlocking {
            val accessToken = getJwt(FIRST_USER_NAME, FIRST_USER_PASSWORD, KEYCLOAK_ADDRESS)

            createHttpClient().use {
                val joinedTopicName = "joined_$SHARED_TOPIC_NAME"
                val response = it.post<CodeExecutionPoll>("http://localhost:8081/joined-resource-code-poll") {
                    header("Authorization", "Bearer ${accessToken.accessToken}")
                    header("Content-Type", "application/json")
                    body = CodeExecutionRequest(
                        resourceName = joinedTopicName,
                        gitUrl = GIT_URL,
                        commitHash = COMMIT_HASH,
                        name = NAME,
                        comment = COMMENT
                    )
                }

                assertEquals(joinedTopicName, response.resourceName)
                assertEquals(GIT_URL, response.gitUrl)
                assertEquals(FIRST_USER_NAME, response.requester)
                assertEquals(COMMIT_HASH, response.commitHash)
                assertEquals(COMMENT, response.comment)
                assertEquals(joinedTopicName, response.resourceName)
                assertEquals(setOf(SECOND_USER), response.actorsToAccept)
                assertEquals(setOf(), response.acceptedActors)
                assertEquals(setOf(), response.declinedActors)

                val secondUserAccessToken = getJwt(SECOND_USER, SECOND_USER_PASSWORD, KEYCLOAK_ADDRESS)
                val acceptResponse =
                    it.post<CodeExecutionPoll>("http://localhost:8081/joined-resource-code-poll/${response.id}/accept") {
                        header("Authorization", "Bearer ${secondUserAccessToken.accessToken}")
                    }

                assertEquals(joinedTopicName, acceptResponse.resourceName)
                assertEquals(GIT_URL, acceptResponse.gitUrl)
                assertEquals(FIRST_USER_NAME, acceptResponse.requester)
                assertEquals(COMMIT_HASH, acceptResponse.commitHash)
                assertEquals(COMMENT, acceptResponse.comment)
                assertEquals(joinedTopicName, acceptResponse.resourceName)
                assertEquals(setOf(SECOND_USER), acceptResponse.actorsToAccept)
                assertEquals(setOf(SECOND_USER), acceptResponse.acceptedActors)
                assertEquals(setOf(), acceptResponse.declinedActors)
            }
        }
    }

    @Test
    fun `custom code execution should have replicated messages to output topic`() {
        runBlocking {
            createHttpClient().use {
                val accessToken = getJwt(FIRST_USER_NAME, FIRST_USER_PASSWORD, "http://keycloak:8080")
                val fullTopicName = "joined_${SHARED_TOPIC_NAME}_${NAME}_$COMMIT_HASH"

                val clientInfo = it.post<ClientInfo>("http://localhost:8081/clients") {
                    header("Authorization", "Bearer ${accessToken.accessToken}")
                    header("Content-Type", "application/json")
                    body = ClientCreationRequest(
                        name = "listener-client",
                        inputResourceIds = listOf(fullTopicName),
                        outputResourceIds = listOf()
                    )
                }

                val consumer = createTestKafkaConsumer(clientInfo.consumerGroup, clientInfo.name, clientInfo.secret)
                consumer.subscribe(listOf(fullTopicName))
                consumer.poll(Duration.ofMillis(1000))

                val firstUserSourceTopic = "$FIRST_USER_NAME.$ALICE_SOURCE_RESOURCE"
                emitToSourceTopic(
                    FIRST_USER_NAME,
                    FIRST_USER_PASSWORD,
                    "first-user-data",
                    firstUserSourceTopic,
                    testMessage
                )

                val secondUserSourceTopic = "$SECOND_USER.$BOB_SOURCE_RESOURCE"
                emitToSourceTopic(
                    SECOND_USER,
                    SECOND_USER_PASSWORD,
                    "second-user-data",
                    secondUserSourceTopic,
                    testMessage
                )

                delay(1000)
                val messages = consumer.poll(Duration.ofMillis(20000))

                messages.records(fullTopicName).forEach { message -> println(message.value()) }

                assertEquals(2, messages.count())
                assertEquals(testMessage, messages.records(fullTopicName).first().value().deserialize(schema))
                assertEquals(testMessage, messages.records(fullTopicName).last().value().deserialize(schema))
            }
        }
    }

    private suspend fun emitToSourceTopic(
        userName: String,
        password: String,
        clientName: String,
        topicName: String,
        message: GenericRecord
    ) {
        val accessToken = getJwt(userName, password, "http://keycloak:8080")

        createHttpClient().use {
            val clientInfo = it.post<ClientInfo>("http://localhost:8081/clients") {
                header("Authorization", "Bearer ${accessToken.accessToken}")
                header("Content-Type", "application/json")
                body = ClientCreationRequest(
                    name = clientName,
                    inputResourceIds = emptyList(),
                    outputResourceIds = listOf(topicName)
                )
            }

            val producer = createTestKafkaProducer(clientInfo.consumerGroup, clientInfo.name, clientInfo.secret)
            val future = producer.send(ProducerRecord(topicName, message.serialize(schema)))

            while (!future.isDone) {
                delay(100)
            }
        }
    }

    companion object {
        private const val ALICE_SOURCE_RESOURCE = "alice-source-topic21"
        private const val BOB_SOURCE_RESOURCE = "bob-source-topic21"
        private const val SHARED_TOPIC_NAME = "shared-topic3121"
        private const val FIRST_USER_NAME = "alice"
        private const val FIRST_USER_PASSWORD = "alice-secret"
        private const val SECOND_USER = "bob"
        private const val SECOND_USER_PASSWORD = "bob-secret"
        private const val KEYCLOAK_ADDRESS = "http://keycloak:8080"
        private const val GIT_URL = "https://github.com/faweizz/poc-joined-resource.git"
        private const val COMMIT_HASH = "a550c613b22a5820dc1b3779cf7a2d42fbd4978a"
        private const val COMMENT = "This is a comment"
        private const val NAME = "Custom-Code-Execution-132"
    }
}