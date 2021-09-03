package de.faweizz.topicservice.adjacent.kafka

import de.faweizz.topicservice.service.Configuration
import java.util.*

class PlainKafkaProperties(configuration: Configuration) : Properties() {
    init {
        setProperty("bootstrap.servers", configuration.kafkaAddress)
        setProperty(
            "sasl.jaas.config",
            "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required oauth.token.endpoint.uri=\"http://keycloak:8080/auth/realms/TestRealm/protocol/openid-connect/token\" oauth.client.id=\"topic-service\" oauth.client.secret=\"topic-service-secret\";"
        )
        setProperty(
            "sasl.login.callback.handler.class",
            "io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler"
        )
        setProperty("security.protocol", "SASL_SSL")
        setProperty("application.id", "topic-service")
        setProperty("sasl.mechanism", "OAUTHBEARER")
        setProperty("ssl.truststore.location", configuration.trustStoreLocation)
        setProperty("ssl.truststore.password", configuration.trustStorePassword)
        setProperty("ssl.endpoint.identification.algorithm", "")

        setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        setProperty("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
        setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
        setProperty("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    }
}