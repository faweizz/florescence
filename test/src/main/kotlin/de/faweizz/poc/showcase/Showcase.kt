package de.faweizz.poc.showcase

import de.faweizz.poc.util.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericRecordBuilder
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Duration
import java.util.*

fun main() {
    val aliceUserName = "alice"
    val aliceSecret = "alice-secret"

    val bobUserName = "bob"
    val bobSecret = "bob-secret"

    val joinedTopicName = "joined-random-vectors"

    val keycloakAddress = "http://keycloak:8080"

    val randomVectorResourceName = "random-vectors"

    val codeExecutionName = "pi_approximation"
    val gitUrl = "https://github.com/faweizz/poc-pi-approximation.git"
    val commitHash = "653597a782636970b4b85458abe8bae21d8bf3f8"

    val outputTopicName = "joined_${joinedTopicName}_${codeExecutionName}_$commitHash"

    val randomVectorSchema = SchemaBuilder.builder()
        .record("random_vector")
        .fields()
        .requiredDouble("x")
        .requiredDouble("y")
        .endRecord()

    val outputSchema = SchemaBuilder.builder()
        .record("pi_approximation")
        .fields()
        .requiredLong("totalVectorsProcessed")
        .requiredLong("vectorsInUnitCircle")
        .requiredDouble("piApproximation")
        .requiredDouble("error")
        .endRecord()

    var exportClient: ClientInfo? = null
    var aliceVectorClient: ClientInfo? = null
    var bobVectorClient: ClientInfo? = null

    runBlocking {
        val aliceAccessToken = getJwt(aliceUserName, aliceSecret, keycloakAddress)
        val bobAccessToken = getJwt(bobUserName, bobSecret, keycloakAddress)

        createHttpClient().use {
            it.post<HttpResponse>("http://localhost:8081/resource") {
                header("Authorization", "Bearer ${aliceAccessToken.accessToken}")
                header("Content-Type", "application/json")
                body = TopicCreationRequest(
                    randomVectorResourceName,
                    randomVectorSchema.toString(),
                    "random vectors between (0,0) and (1,1)"
                )
            }

            it.post<HttpResponse>("http://localhost:8081/resource") {
                header("Authorization", "Bearer ${bobAccessToken.accessToken}")
                header("Content-Type", "application/json")
                body = TopicCreationRequest(
                    randomVectorResourceName,
                    randomVectorSchema.toString(),
                    "random vectors between (0,0) and (1,1)"
                )
            }

            it.post<JoinedResource>("http://localhost:8081/joined-resource") {
                header("Authorization", "Bearer ${aliceAccessToken.accessToken}")
                header("Content-Type", "application/json")
                body = JoinedResourceCreationRequest(
                    name = joinedTopicName,
                    sourceResource = "alice.$randomVectorResourceName",
                    description = "random vectors between (0,0) and (1,1)"
                )
            }

            val joinedResourceName = "joined_$joinedTopicName"

            val poll = it.post<JoinedResourceJoinRequest>("http://localhost:8081/joined-resource-poll") {
                header("Authorization", "Bearer ${bobAccessToken.accessToken}")
                header("Content-Type", "application/json")
                body = JoinedTopicJoinRequestRequest(
                    joinedResourceName,
                    "bob.$randomVectorResourceName"
                )
            }

            it.post<JoinedResourceJoinRequest>("http://localhost:8081/joined-resource-poll/${poll.id}/accept") {
                header("Authorization", "Bearer ${aliceAccessToken.accessToken}")
            }

            val codeExecutionPoll = it.post<CodeExecutionPoll>("http://localhost:8081/joined-resource-code-poll") {
                header("Authorization", "Bearer ${aliceAccessToken.accessToken}")
                header("Content-Type", "application/json")
                body = CodeExecutionRequest(
                    resourceName = joinedResourceName,
                    gitUrl = gitUrl,
                    commitHash = commitHash,
                    name = codeExecutionName,
                    comment = "no comment"
                )
            }

            it.post<CodeExecutionPoll>("http://localhost:8081/joined-resource-code-poll/${codeExecutionPoll.id}/accept") {
                header("Authorization", "Bearer ${bobAccessToken.accessToken}")
            }

            //create export service
            exportClient = it.post<ClientInfo>("http://localhost:8081/clients") {
                header("Authorization", "Bearer ${aliceAccessToken.accessToken}")
                header("Content-Type", "application/json")
                body = ClientCreationRequest(
                    name = "pi-listener-client",
                    inputResourceIds = listOf(outputTopicName),
                    outputResourceIds = listOf()
                )
            }

            //create vector service 1
            aliceVectorClient = it.post<ClientInfo>("http://localhost:8081/clients") {
                header("Authorization", "Bearer ${aliceAccessToken.accessToken}")
                header("Content-Type", "application/json")
                body = ClientCreationRequest(
                    name = "vector-client",
                    inputResourceIds = listOf(),
                    outputResourceIds = listOf("alice.$randomVectorResourceName")
                )
            }

            //create vector service 2
            bobVectorClient = it.post<ClientInfo>("http://localhost:8081/clients") {
                header("Authorization", "Bearer ${bobAccessToken.accessToken}")
                header("Content-Type", "application/json")
                body = ClientCreationRequest(
                    name = "vector-client",
                    inputResourceIds = listOf(),
                    outputResourceIds = listOf("bob.$randomVectorResourceName")
                )
            }
        }
    }

    val safeExportClient = exportClient ?: throw Exception()
    val safeAliceVectorClient = aliceVectorClient ?: throw Exception()
    val safeBobVectorClient = bobVectorClient ?: throw Exception()

    println(safeExportClient)
    println(safeAliceVectorClient)
    println(safeBobVectorClient)

    val exportConsumer =
        createTestKafkaConsumer(safeExportClient.consumerGroup, safeExportClient.name, safeExportClient.secret)
    exportConsumer.subscribe(listOf(outputTopicName))

    val aliceVectorProducer = createTestKafkaProducer(
        safeAliceVectorClient.consumerGroup,
        safeAliceVectorClient.name,
        safeAliceVectorClient.secret
    )

    val bobVectorProducer = createTestKafkaProducer(
        safeBobVectorClient.consumerGroup,
        safeBobVectorClient.name,
        safeBobVectorClient.secret
    )

    GlobalScope.launch {
        val random = Random()
        while (this.isActive) {
            val vector = GenericRecordBuilder(randomVectorSchema)
                .set("x", random.nextDouble())
                .set("y", random.nextDouble())
                .build()

            aliceVectorProducer.send(ProducerRecord("alice.$randomVectorResourceName", vector.serialize(vector.schema)))
            delay(100)
        }
    }

    GlobalScope.launch {
        val random = Random()
        while (this.isActive) {
            val vector = GenericRecordBuilder(randomVectorSchema)
                .set("x", random.nextDouble())
                .set("y", random.nextDouble())
                .build()

            bobVectorProducer.send(ProducerRecord("bob.$randomVectorResourceName", vector.serialize(vector.schema)))
            delay(100)
        }
    }

    while (true) {
        val results = exportConsumer.poll(Duration.ofMillis(1000))
        results.forEach {
            val decoded = it.value().deserialize(outputSchema)
            println(decoded)
        }
    }
}
