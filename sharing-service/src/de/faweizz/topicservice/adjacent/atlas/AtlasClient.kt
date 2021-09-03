package de.faweizz.topicservice.adjacent.atlas

import de.faweizz.topicservice.service.client.Client
import de.faweizz.topicservice.service.transformation.Transformation
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.apache.atlas.AtlasClientV2
import org.apache.atlas.model.instance.AtlasEntity
import org.apache.avro.Schema
import javax.ws.rs.core.Cookie

class AtlasClient(
    private val atlasAddress: String,
    private val kafkaAddress: String,
    private val keycloakAddress: String
) {

    private fun createAvroSchemaEntity(client: AtlasClientV2, id: String, schema: String): String? {
        val avroSchema = Schema.Parser().parse(schema)

        val fullSchemaName = "${id}.${avroSchema.name}"

        val schemaBody = AtlasEntity().apply {
            status = AtlasEntity.Status.ACTIVE
            createdBy = "topic-service"
            typeName = "avro_schema"
            attributes = mapOf(
                "name" to avroSchema.name,
                "qualifiedName" to fullSchemaName,
                "namespace" to (avroSchema.namespace ?: "empty_namespace"),
                "type" to avroSchema.type.toString(),
                "value" to avroSchema.toString(),
                "avro_notation" to avroSchema.toString()
            )
        }

        return client.createEntities(AtlasEntity.AtlasEntitiesWithExtInfo(schemaBody)).guidAssignments.entries.first().value
    }

    suspend fun createResourceEntity(id: String, schema: String, description: String) {
        val client = AtlasClientV2(arrayOf(atlasAddress), createSessionCookie())

        val schemaId = createAvroSchemaEntity(client, id, schema)
        val body = AtlasEntity.AtlasEntityWithExtInfo().apply {
            entity = AtlasEntity()
            entity.status = AtlasEntity.Status.ACTIVE
            entity.createdBy = "topic-service"
            entity.typeName = "kafka_topic"
            entity.attributes = mapOf(
                "qualifiedName" to id,
                "name" to id,
                "topic" to id,
                "uri" to "${id}@$kafkaAddress",
                "avroSchema" to listOf(mapOf("guid" to schemaId)),
                "description" to description
            )
        }

        client.createEntity(body)
    }

    suspend fun createTransformationEntity(transformation: Transformation): AtlasId {
        val client = AtlasClientV2(arrayOf(atlasAddress), createSessionCookie())

        val sourceTopicGuid = client.getEntityByAttribute(
            "kafka_topic",
            mapOf(
                "qualifiedName" to transformation.sourceTopicName
            )
        ).entity.guid

        val targetTopicGuid = client.getEntityByAttribute(
            "kafka_topic",
            mapOf(
                "qualifiedName" to transformation.targetTopicName
            )
        ).entity.guid

        val transformationName = "${transformation.sourceTopicName} -> ${transformation.targetTopicName}"

        val body = AtlasEntity.AtlasEntityWithExtInfo().apply {
            entity = AtlasEntity()
            entity.status = AtlasEntity.Status.ACTIVE
            entity.createdBy = "topic-service"
            entity.typeName = "Process"
            entity.attributes = mapOf(
                "qualifiedName" to transformationName,
                "name" to transformationName,
                "inputs" to listOf(
                    mapOf(
                        "guid" to sourceTopicGuid
                    )
                ),
                "outputs" to listOf(
                    mapOf(
                        "guid" to targetTopicGuid
                    )
                )
            )
        }

        val response = client.createEntity(body)
        return AtlasId(response.createdEntities.first().guid)
    }

    private suspend fun createSessionCookie(): Cookie {
        io.ktor.client.HttpClient(CIO) {
            install(Logging)
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }.use { client ->
            val grant =
                client.post<OpenIdGrant>("$keycloakAddress/auth/realms/TestRealm/protocol/openid-connect/token") {
                    body = FormDataContent(
                        Parameters.build {
                            append("grant_type", "client_credentials")
                            append("scope", "openid")
                            append("client_id", "topic-service")
                            append("client_secret", "topic-service-secret")
                        }
                    )
                }

            val response = client.post<HttpResponse>(atlasAddress) {
                headers.append("Authorization", "Bearer ${grant.accessToken}")
            }

            val setCookies = response.headers["Set-Cookie"] ?: throw Exception("Login failed")

            val sessionId = setCookies.split(",").first()
            val values = sessionId.split("=")
            return Cookie(values[0], values[1].split(";").first())
        }
    }

    suspend fun createClientTransformation(clientToCreate: Client) {
        val atlasClient = AtlasClientV2(arrayOf(atlasAddress), createSessionCookie())

        val sourceTopicsGuids = clientToCreate.inputResourceIds.map {
            atlasClient.getEntityByAttribute(
                "kafka_topic",
                mapOf(
                    "qualifiedName" to it
                )
            ).entity.guid
        }

        val targetTopicIds = clientToCreate.outputResourceIds.map {
            atlasClient.getEntityByAttribute(
                "kafka_topic",
                mapOf(
                    "qualifiedName" to it
                )
            ).entity.guid
        }

        val body = AtlasEntity.AtlasEntityWithExtInfo().apply {
            entity = AtlasEntity()
            entity.status = AtlasEntity.Status.ACTIVE
            entity.createdBy = "topic-service"
            entity.typeName = "Process"
            entity.attributes = mapOf(
                "qualifiedName" to clientToCreate.name,
                "name" to clientToCreate.name,
                "inputs" to sourceTopicsGuids.map {
                    mapOf(
                        "guid" to it
                    )
                },
                "outputs" to targetTopicIds.map {
                    mapOf(
                        "guid" to it
                    )
                }
            )
        }

        atlasClient.createEntity(body)
    }
}