package de.faweizz.topicservice.adjacent.keycloak

import de.faweizz.topicservice.service.resource.AccessLevel
import io.ktor.http.*
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation
import org.keycloak.representations.idm.authorization.ResourceRepresentation
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation
import org.keycloak.representations.idm.authorization.ScopeRepresentation


class KeycloakTopicAuthorizationClient(
    private val keycloak: RealmResource
) {

    fun grantAccess(topic: String, clientId: String, scope: AccessLevel) {
        if (scope == AccessLevel.NONE) return

        val keycloakClients = keycloak.clients().findAll()

        val kafkaClient = keycloakClients.find { it.clientId == "kafka" } ?: throw Exception("Up")
        val realmClient = keycloak.clients().get(kafkaClient.id)
        val authorizationClient = realmClient.authorization()

        val resource = ResourceRepresentation().apply {
            name = "Topic:$topic"
            type = "Topic"
            scopes = setOf(
                ScopeRepresentation("Read"),
                ScopeRepresentation("Write"),
                ScopeRepresentation("Describe")
            )
        }

        val resourceClient = authorizationClient.resources()

        val resourceResponse = resourceClient.create(resource)
        if (resourceResponse.status != HttpStatusCode.Created.value && resourceResponse.status != HttpStatusCode.Conflict.value) {
            throw Exception(resourceResponse.statusInfo.reasonPhrase)
        }

        val policyClient = authorizationClient.policies()

        val policy = ClientPolicyRepresentation().apply {
            name = "Client $clientId"
            addClient(clientId)
        }

        val existingPolicy = policyClient.client().findByName(policy.name)

        if (existingPolicy == null) {
            val policyResponse = policyClient.client().create(policy)
            if (policyResponse.status != HttpStatusCode.Created.value) {
                throw Exception(policyResponse.statusInfo.reasonPhrase)
            }
        }

        val permissionClient = authorizationClient.permissions().scope()

        val scopesToGrant = when (scope) {
            AccessLevel.WRITE -> setOf("Read", "Write", "Describe")
            AccessLevel.NONE -> return
            AccessLevel.READ -> setOf("Read", "Describe")
        }

        val resourcePermissionRepresentation = ScopePermissionRepresentation().apply {
            name = "${policy.name} can access ${resource.name}"
            resources = setOf(resource.name)
            policies = setOf(policy.name)
            scopes = scopesToGrant
        }

        val permissionResponse = permissionClient.create(resourcePermissionRepresentation)
        if (permissionResponse.status != 201) {
            throw Exception(permissionResponse.statusInfo.reasonPhrase)
        }
    }
}