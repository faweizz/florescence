package de.faweizz.topicservice.service.auditlogging

import de.faweizz.topicservice.adjacent.kafka.KafkaClient
import de.faweizz.topicservice.service.transformation.serialize
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericRecordBuilder

class AuditLogger(
    private val kafkaClient: KafkaClient
) {

    fun logResourceCreation(name: String, actorName: String) {
        val schema = SchemaBuilder.builder()
            .record("resourcecreation")
            .fields()
            .requiredString("name")
            .requiredString("actorName")
            .endRecord()

        val message = GenericRecordBuilder(schema)
            .set("name", name)
            .set("actorName", actorName)
            .build()
            .serialize()

        kafkaClient.send("internal.resource-creation", message)
    }

    fun logDataSharingRequest(id: String, resourceName: String, toConfirmByActors: List<String>) {
        val schema = SchemaBuilder.builder()
            .record("datasharingrequest")
            .fields()
            .requiredString("id")
            .requiredString("resourceName")
            .requiredString("toConfirmByActors")
            .endRecord()

        val message = GenericRecordBuilder(schema)
            .set("id", id)
            .set("resourceName", resourceName)
            .set("toConfirmByActors", toConfirmByActors.joinToString(","))
            .build()
            .serialize()

        kafkaClient.send("internal.data-sharing-request", message)
    }

    fun logDataSharingRequestAccept(id: String, actorName: String) {
        val schema = SchemaBuilder.builder()
            .record("datasharingrequestaccept")
            .fields()
            .requiredString("id")
            .requiredString("actorName")
            .endRecord()

        val message = GenericRecordBuilder(schema)
            .set("id", id)
            .set("actorName", actorName)
            .build()
            .serialize()

        kafkaClient.send("internal.data-sharing-request-accept", message)
    }

    fun logDataSharingRequestDecline(id: String, actorName: String) {
        val schema = SchemaBuilder.builder()
            .record("datasharingrequestdecline")
            .fields()
            .requiredString("id")
            .requiredString("actorName")
            .endRecord()

        val message = GenericRecordBuilder(schema)
            .set("id", id)
            .set("actorName", actorName)
            .build()
            .serialize()

        kafkaClient.send("internal.data-sharing-request-decline", message)
    }

    fun logClientCreation(
        clientName: String,
        actorName: String,
        inputResources: List<String>,
        outputResources: List<String>
    ) {
        val schema = SchemaBuilder.builder()
            .record("clientcreation")
            .fields()
            .requiredString("clientName")
            .requiredString("actorName")
            .requiredString("inputResources")
            .requiredString("outputResources")
            .endRecord()

        val message = GenericRecordBuilder(schema)
            .set("clientName", clientName)
            .set("actorName", actorName)
            .set("inputResources", inputResources.joinToString(","))
            .set("outputResources", outputResources.joinToString(","))
            .build()
            .serialize()

        kafkaClient.send("internal.client-creation", message)
    }

    fun logJoinedResourceCreation(resourceName: String, actorName: String) {
        val schema = SchemaBuilder.builder()
            .record("joinedresourcecreation")
            .fields()
            .requiredString("resourceName")
            .requiredString("actorName")
            .endRecord()

        val message = GenericRecordBuilder(schema)
            .set("resourceName", resourceName)
            .set("actorName", actorName)
            .build()
            .serialize()

        kafkaClient.send("internal.joined-resource-creation", message)
    }

    fun logJoinedResourceJoinRequest(
        id: String,
        resourceName: String,
        actorName: String,
        toConfirmByActors: List<String>
    ) {
        val schema = SchemaBuilder.builder()
            .record("joinedresourcejoinrequest")
            .fields()
            .requiredString("id")
            .requiredString("resourceName")
            .requiredString("actorName")
            .requiredString("toConfirmByActors")
            .endRecord()

        val message = GenericRecordBuilder(schema)
            .set("id", id)
            .set("resourceName", resourceName)
            .set("actorName", actorName)
            .set("toConfirmByActors", toConfirmByActors.joinToString(","))
            .build()
            .serialize()

        kafkaClient.send("internal.joined-resource-join-request", message)
    }

    fun logJoinedResourceJoinRequestAccept(id: String, actorName: String) {
        val schema = SchemaBuilder.builder()
            .record("joinedresourcejoinrequestaccept")
            .fields()
            .requiredString("id")
            .requiredString("actorName")
            .endRecord()

        val message = GenericRecordBuilder(schema)
            .set("id", id)
            .set("actorName", actorName)
            .build()
            .serialize()

        kafkaClient.send("internal.joined-resource-join-request-accept", message)
    }

    fun logJoinedResourceJoinRequestDecline(id: String, actorName: String) {
        val schema = SchemaBuilder.builder()
            .record("joinedresourcejoinrequestdecline")
            .fields()
            .requiredString("id")
            .requiredString("actorName")
            .endRecord()

        val message = GenericRecordBuilder(schema)
            .set("id", id)
            .set("actorName", actorName)
            .build()
            .serialize()

        kafkaClient.send("internal.joined-resource-join-request-decline", message)
    }

    fun logJoinedResourceComputeRequest(id: String, actorName: String, toConfirmByActors: List<String>) {
        val schema = SchemaBuilder.builder()
            .record("joinedresourcecomputerequest")
            .fields()
            .requiredString("id")
            .requiredString("actorName")
            .requiredString("toConfirmByActors")
            .endRecord()

        val message = GenericRecordBuilder(schema)
            .set("id", id)
            .set("actorName", actorName)
            .set("toConfirmByActors", toConfirmByActors.joinToString(","))
            .build()
            .serialize()

        kafkaClient.send("internal.joined-resource-compute-request", message)
    }

    fun logJoinedResourceComputeRequestAccept(id: String, actorName: String) {
        val schema = SchemaBuilder.builder()
            .record("joinedresourcecomputerequestaccept")
            .fields()
            .requiredString("id")
            .requiredString("actorName")
            .endRecord()

        val message = GenericRecordBuilder(schema)
            .set("id", id)
            .set("actorName", actorName)
            .build()
            .serialize()

        kafkaClient.send("internal.joined-resource-compute-request-accept", message)
    }

    fun logJoinedResourceComputeRequestDecline(id: String, actorName: String) {
        val schema = SchemaBuilder.builder()
            .record("joinedresourcecomputerequestdecline")
            .fields()
            .requiredString("id")
            .requiredString("actorName")
            .endRecord()

        val message = GenericRecordBuilder(schema)
            .set("id", id)
            .set("actorName", actorName)
            .build()
            .serialize()

        kafkaClient.send("internal.joined-resource-compute-request-decline", message)
    }
}