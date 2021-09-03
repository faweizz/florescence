package de.faweizz.topicservice.service.resource

import de.faweizz.topicservice.Actor
import de.faweizz.topicservice.adjacent.atlas.AtlasClient
import de.faweizz.topicservice.adjacent.kafka.KafkaClient
import de.faweizz.topicservice.persistence.Mongo
import de.faweizz.topicservice.service.auditlogging.AuditLogger
import org.apache.avro.Schema

class ResourceService(
    private val atlasClient: AtlasClient,
    private val mongo: Mongo,
    private val kafkaClient: KafkaClient,
    private val auditLogger: AuditLogger
) {

    suspend fun executeCreationRequest(topicCreationRequest: TopicCreationRequest, actor: Actor) {
        val fullyQualifiedName = "${actor.name}.${topicCreationRequest.topicName}"
        val schema = Schema.Parser().parse(topicCreationRequest.schema)
        createResource(
            fullyQualifiedName,
            listOf(actor.name),
            schema = schema,
            description = topicCreationRequest.description
        )

        auditLogger.logResourceCreation(fullyQualifiedName, actor.name)
    }

    suspend fun createResource(
        fullyQualifiedName: String,
        owningActorIds: List<String>,
        accessLevel: AccessLevel = AccessLevel.WRITE,
        schema: Schema,
        description: String
    ): Resource {
        val topic = Resource(
            id = fullyQualifiedName,
            owningActors = owningActorIds,
            ownerAccessLevel = accessLevel,
            schema = schema.toString(),
            description = description
        )

        mongo.findResourceById(fullyQualifiedName)?.let { throw Exception("Topic ${it.id} already exists") }

        kafkaClient.createTopic(topic.id)
        atlasClient.createResourceEntity(topic.id, topic.schema, description)

        mongo.saveTopic(topic)
        return topic
    }
}