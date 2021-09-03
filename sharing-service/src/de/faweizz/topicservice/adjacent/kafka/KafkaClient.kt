package de.faweizz.topicservice.adjacent.kafka

import de.faweizz.topicservice.service.Configuration
import de.faweizz.topicservice.service.transformation.KafkaProperties
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.admin.KafkaAdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class KafkaClient(
    private val configuration: Configuration
) {

    fun createTopic(topicName: String) {
        val adminClient = KafkaAdminClient.create(KafkaProperties(configuration))
        val topics = adminClient.listTopics()
            .names()
            .get()

        if (topics.contains(topicName)) return

        adminClient.createTopics(listOf(NewTopic(topicName, 1, 1)))
            .all()
            .get()

        adminClient.close()
    }

    fun send(topicName: String, bytes: ByteArray) {
        KafkaProducer<String, ByteArray>(PlainKafkaProperties(configuration)).send(ProducerRecord(topicName, bytes))
    }
}