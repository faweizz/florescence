package de.faweizz.topicservice.adjacent.keycloak

import de.faweizz.topicservice.service.client.Client
import de.faweizz.topicservice.service.resource.AccessLevel
import org.keycloak.OAuth2Constants
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.admin.client.resource.RealmResource

class KeycloakClient {

    fun authorizeClient(client: Client): CreatedClient {
        val keycloak = connectToKeycloak()

        val createdClient = KeycloakClientCreationClient(keycloak).createClient(client)

        val keycloakAuthorizationClient = KeycloakTopicAuthorizationClient(keycloak)
        client.inputResourceIds.forEach {
            keycloakAuthorizationClient.grantAccess(
                it,
                createdClient.id,
                AccessLevel.READ
            )
        }

        client.outputResourceIds.forEach {
            keycloakAuthorizationClient.grantAccess(
                it,
                createdClient.id,
                AccessLevel.WRITE
            )
        }

        val consumerGroupName = "${client.name}-consumer-group"
        KeycloakGroupAuthorizationClient(keycloak).authorizeClientOnGroup(
            createdClient.id,
            consumerGroupName
        )

        return CreatedClient(
            name = createdClient.id,
            secret = createdClient.secret,
            consumerGroup = consumerGroupName
        )
    }

    private fun connectToKeycloak(): RealmResource {
        return KeycloakBuilder.builder()
            .serverUrl("http://keycloak:8080/auth")
            .realm("TestRealm")
            .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
            .clientId("topic-service")
            .clientSecret("topic-service-secret")
            .build()
            .realm("TestRealm")
    }
}