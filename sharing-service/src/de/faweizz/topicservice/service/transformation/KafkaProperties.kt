package de.faweizz.topicservice.service.transformation

import de.faweizz.topicservice.service.Configuration
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import java.util.*

class KafkaProperties(configuration: Configuration) : Properties() {
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

        setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String()::class.java.name)
        setProperty(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.ByteArray()::class.java.name)
    }
}