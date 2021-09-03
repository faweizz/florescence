package de.faweizz.poc.util

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import java.net.InetAddress
import java.util.*

fun createHttpClient(): HttpClient {
    return HttpClient(CIO) {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
        }
    }
}

suspend fun getJwt(username: String, password: String, keycloakAddress: String): OpenIdGrant {
    return createHttpClient().use {
        it.post("$keycloakAddress/auth/realms/TestRealm/protocol/openid-connect/token") {
            body = FormDataContent(
                Parameters.build {
                    append("grant_type", "password")
                    append("scope", "openid")
                    append("client_id", "postman")
                    append("client_secret", "postman-secret")
                    append("username", username)
                    append("password", password)
                }
            )
        }
    }
}

fun createTestKafkaConsumer(
    consumerGroup: String,
    serviceName: String,
    serviceSecret: String
): KafkaConsumer<String, ByteArray> {
    return KafkaConsumer<String, ByteArray>(createTestKafkaConfig(consumerGroup, serviceName, serviceSecret))
}

fun createTestKafkaProducer(
    consumerGroup: String,
    serviceName: String,
    serviceSecret: String
): KafkaProducer<String, ByteArray> {
    return KafkaProducer<String, ByteArray>(createTestKafkaConfig(consumerGroup, serviceName, serviceSecret))
}

fun createTestKafkaConfig(consumerGroup: String, serviceName: String, serviceSecret: String): Properties {
    val config = Properties()
    config["client.id"] = InetAddress.getLocalHost().hostName
    config["group.id"] = consumerGroup
    config.setProperty("bootstrap.servers", "localhost:9092")
    config.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    config.setProperty("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    config.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    config.setProperty("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    config["sasl.jaas.config"] =
        "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required oauth.token.endpoint.uri=\"http://keycloak:8080/auth/realms/TestRealm/protocol/openid-connect/token\" oauth.client.id=\"$serviceName\" oauth.client.secret=\"$serviceSecret\";"
    config["sasl.login.callback.handler.class"] = "io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler"
    config["security.protocol"] = "SASL_SSL"
    config["sasl.mechanism"] = "OAUTHBEARER"
    config["ssl.truststore.location"] = "../broker/cert/kafka.client.truststore.jks"
    config["ssl.truststore.password"] = "password"
    config["ssl.endpoint.identification.algorithm"] = ""

    return config
}