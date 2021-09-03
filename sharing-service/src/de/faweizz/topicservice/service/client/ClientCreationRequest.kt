package de.faweizz.topicservice.service.client

data class ClientCreationRequest(
    val name: String,
    val inputResourceIds: List<String>,
    val outputResourceIds: List<String>
)