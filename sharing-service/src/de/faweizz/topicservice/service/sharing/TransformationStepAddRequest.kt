package de.faweizz.topicservice.service.sharing

data class TransformationStepAddRequest(
    val transformationStepId: String,
    val data: Map<String, String>
)