package de.faweizz.topicservice.service.client

import de.faweizz.topicservice.Actor
import de.faweizz.topicservice.adjacent.atlas.AtlasClient
import de.faweizz.topicservice.adjacent.keycloak.CreatedClient
import de.faweizz.topicservice.adjacent.keycloak.KeycloakClient
import de.faweizz.topicservice.persistence.Mongo
import de.faweizz.topicservice.service.auditlogging.AuditLogger
import de.faweizz.topicservice.service.resource.AccessLevel
import de.faweizz.topicservice.service.resource.Resource

class ClientService(
    private val mongo: Mongo,
    private val keycloakClient: KeycloakClient,
    private val atlasClient: AtlasClient,
    private val auditLogger: AuditLogger
) {

    suspend fun createClientIfActorIsPermitted(
        actor: Actor,
        clientCreationRequest: ClientCreationRequest
    ): CreatedClient {
        val inputResources = clientCreationRequest.inputResourceIds
            .map { mongo.findResourceById(it) }
            .apply { if (contains(null)) throw Exception("Unknown input resource requested") }
            .filterNotNull()

        if (!isActorAuthorizedToRead(actor, inputResources))
            throw Exception("Actor not authorized to access all of the input resources")

        val outputResources = clientCreationRequest.outputResourceIds
            .map { mongo.findResourceById(it) }
            .apply { if (contains(null)) throw Exception("Unknown output resource requested") }
            .filterNotNull()

        if (!isActorAuthorizedToWrite(actor, outputResources))
            throw Exception("Actor not authorized to access all of the output resources")

        val name = "${actor.name}.${clientCreationRequest.name}"

        val client = createClient(name, clientCreationRequest.inputResourceIds, clientCreationRequest.outputResourceIds)

        auditLogger.logClientCreation(
            name,
            actor.name,
            clientCreationRequest.inputResourceIds,
            clientCreationRequest.outputResourceIds
        )

        return client
    }

    suspend fun createClient(
        name: String,
        inputResourceIds: List<String>,
        outputResourceIds: List<String>
    ): CreatedClient {
        val client = Client(
            name = name,
            inputResourceIds = inputResourceIds,
            outputResourceIds = outputResourceIds
        )

        if (mongo.findClient(client.name) != null) throw Exception("Client already exists")

        val createdClient = keycloakClient.authorizeClient(client)
        atlasClient.createClientTransformation(client)

        mongo.saveClient(client)
        return createdClient
    }

    private fun isActorAuthorizedToRead(actor: Actor, inputResources: List<Resource>): Boolean {
        return inputResources.all { hasAccess(it, actor, AccessLevel.READ) }
    }

    private fun isActorAuthorizedToWrite(actor: Actor, outputResources: List<Resource>): Boolean {
        return outputResources.all { hasAccess(it, actor, AccessLevel.WRITE) }
    }

    private fun hasAccess(resource: Resource, actor: Actor, accessLevel: AccessLevel): Boolean {
        if (resource.hasOwnerAccess(actor, accessLevel)) return true
        mongo.findTransactionConcerning(actor.name, resource.id)?.let {
            return it.grantedAccessLevel.isHigherOrEqualThan(accessLevel)
        }

        return false
    }
}