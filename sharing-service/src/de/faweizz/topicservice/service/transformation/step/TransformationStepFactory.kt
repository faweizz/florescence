package de.faweizz.topicservice.service.transformation.step

import de.faweizz.topicservice.service.transformation.TransformationStepData

class TransformationStepFactory {
    fun instantiate(transformationStepData: TransformationStepData): TransformationStep {
        val clazz = Class.forName(transformationStepData.transformationStepName)
        val constructor = clazz.getConstructor(Map::class.java)
        return constructor.newInstance(transformationStepData.data) as TransformationStep
    }
}