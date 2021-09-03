package de.faweizz.topicservice.service.transformation

import org.apache.avro.Schema
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.KStream
import java.util.*

data class Transformation(
    val id: String = UUID.randomUUID().toString(),
    val sourceTopicName: String,
    val targetTopicName: String,
    val transformationStepData: List<TransformationStepData>
) {

    fun init(sourceTopicStream: KStream<String, ByteArray>, sourceSchemaString: String) {
        val sourceSchema = Schema.Parser().parse(sourceSchemaString)

        val transformationExecutor = TransformationExecutor(transformationStepData)

        sourceTopicStream
            .map { key, value ->
                val sourceData = value.deserialize(sourceSchema)
                val sourceDataString = sourceData.toString()

                val targetData = transformationExecutor.apply(sourceData)

                println("($key, $sourceDataString) transformed to ($key, $targetData) from $sourceTopicName to $targetTopicName")

                KeyValue(key, targetData.serialize())
            }
            .to(targetTopicName)
    }
}