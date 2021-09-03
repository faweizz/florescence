package de.faweizz.topicservice.adjacent.keycloak

import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.representations.idm.authorization.ClientPolicyRepresentation
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation
import org.keycloak.representations.idm.authorization.ResourceRepresentation
import org.keycloak.representations.idm.authorization.ScopeRepresentation

class KeycloakGroupAuthorizationClient(
    private val keycloak: RealmResource
) {

    fun authorizeClientOnGroup(clientId: String, groupName: String) {
        val keycloakClients = keycloak.clients().findAll()

        val kafkaClient = keycloakClients.find { it.clientId == "kafka" } ?: throw Exception("Up")
        val realmClient = keycloak.clients().get(kafkaClient.id)
        val authorizationClient = realmClient.authorization()

        val resource = ResourceRepresentation().apply {
            name = "Group:$groupName"
            type = "Group"
            scopes = mutableSetOf(ScopeRepresentation("Read"), ScopeRepresentation("Describe"))
        }

        val resourceClient = authorizationClient.resources()

        val resourceResponse = resourceClient.create(resource)
        if (resourceResponse.status != 201) {
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
            if (policyResponse.status != 201) {
                throw  Exception(policyResponse.statusInfo.reasonPhrase)
            }
        }

        val permissionClient = authorizationClient.permissions().resource()

        val resourcePermissionRepresentation = ResourcePermissionRepresentation().apply {
            name = "${policy.name} can access ${resource.name}"
            resources = setOf(resource.name)
            policies = setOf(policy.name)
        }

        val permissionResponse = permissionClient.create(resourcePermissionRepresentation)
        if (permissionResponse.status != 201) {
            throw  Exception(permissionResponse.statusInfo.reasonPhrase)
        }
    }
}