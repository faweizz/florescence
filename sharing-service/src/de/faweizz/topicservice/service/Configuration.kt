package de.faweizz.topicservice.service

data class Configuration(
    val trustStoreLocation: String,
    val trustStorePassword: String,
    val kafkaAddress: String
)