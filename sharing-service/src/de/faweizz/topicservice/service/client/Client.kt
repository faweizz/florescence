package de.faweizz.topicservice.service.client

data class Client(
    val name: String,
    val inputResourceIds: List<String>,
    val outputResourceIds: List<String>
)