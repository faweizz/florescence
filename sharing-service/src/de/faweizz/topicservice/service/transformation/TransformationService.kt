package de.faweizz.topicservice.service.transformation

import de.faweizz.topicservice.adjacent.atlas.AtlasClient
import de.faweizz.topicservice.persistence.Mongo
import de.faweizz.topicservice.service.Configuration
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder

class TransformationService(
    private val mongo: Mongo,
    private val atlasClient: AtlasClient,
    private val configuration: Configuration
) {

    private var currentStreamsApplication: KafkaStreams? = null

    init {
        initializeStreams()
    }

    private fun initializeStreams() {
        currentStreamsApplication?.close()

        val builder = StreamsBuilder()
        val allTransformations = mongo.getAllTransformations()
        if (allTransformations.isEmpty()) return

        val allSourceTopics = allTransformations.map { it.sourceTopicName }.distinct()
        val allSources = allSourceTopics.associateWith { builder.stream<String, ByteArray>(it) }

        allTransformations.forEach {
            val sourceTopicSchema = mongo.findResourceById(it.sourceTopicName)?.schema
                ?: mongo.findJoinedResourceByName(it.sourceTopicName)?.schema
                ?: throw Exception("Unknown topic in transformation ${it.sourceTopicName}")

            it.init(allSources[it.sourceTopicName]!!, sourceTopicSchema)
        }

        val newStreams = KafkaStreams(builder.build(), KafkaProperties(configuration))
        newStreams.setUncaughtExceptionHandler { _, e ->
            println("Received an error from transformations: $e, restarting...")
            e.printStackTrace()
            initializeStreams()
        }
        newStreams.start()

        currentStreamsApplication = newStreams
    }

    suspend fun createTransformation(
        fromTopic: String,
        toTopic: String,
        transformationStepData: List<TransformationStepData> = emptyList()
    ): Transformation {
        val transformation = Transformation(
            sourceTopicName = fromTopic,
            targetTopicName = toTopic,
            transformationStepData = transformationStepData
        )

        atlasClient.createTransformationEntity(transformation)
        mongo.saveTransformation(transformation)

        initializeStreams()
        return transformation
    }
}