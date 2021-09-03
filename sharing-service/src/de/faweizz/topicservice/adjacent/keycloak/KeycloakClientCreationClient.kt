package de.faweizz.topicservice.adjacent.keycloak

import de.faweizz.topicservice.service.client.Client
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.idm.ClientRepresentation
import java.util.*

class KeycloakClientCreationClient(
    private val keycloak: RealmResource
) {

    fun createClient(client: Client): CreatedKeycloakClient {
        val keyCloakClient = ClientRepresentation().apply {
            id = client.name
            isPublicClient = false
            isServiceAccountsEnabled = true
            secret = UUID.randomUUID().toString()
        }

        keycloak.clients().create(keyCloakClient)

        return CreatedKeycloakClient(keyCloakClient.id, keyCloakClient.secret)
    }
}