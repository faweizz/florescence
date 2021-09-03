package de.faweizz.topicservice.service.sharing

import de.faweizz.topicservice.service.transformation.TransformationStepData
import java.util.*

data class PendingTransformationStep(
    val id: String = UUID.randomUUID().toString(),
    val step: TransformationStepData
)