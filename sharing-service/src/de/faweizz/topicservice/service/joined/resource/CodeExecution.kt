package de.faweizz.topicservice.service.joined.resource

data class CodeExecution(
    val name: String,
    val submittedByActor: String,
    val gitlabRepositoryUrl: String,
    val commitHash: String,
    val clientName: String,
    val clientSecret: String,
    val clientConsumerGroup: String,
    val inputTopic: String,
    val outputTopic: String
)