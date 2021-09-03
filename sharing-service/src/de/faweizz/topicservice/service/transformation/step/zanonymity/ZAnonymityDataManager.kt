/*
Implementation of algorithm proposed in https://ieeexplore.ieee.org/document/9378422
 */

package de.faweizz.topicservice.service.transformation.step.zanonymity

class ZAnonymityDataManager(
    private val z: Int,
    private val deltaMillis: Long
) {

    private val evidenceMap = HashMap<ShownAttribute, AttributeEvidence>()

    fun isFieldZAnonymous(timeMillis: Long, personId: String, attributeName: String, value: Any): Boolean {
        val shownAttribute = ShownAttribute(attributeName, value)

        if (!evidenceMap.containsKey(shownAttribute)) {
            val newEvidence = AttributeEvidence()
            evidenceMap[shownAttribute] = newEvidence

            newEvidence.witnessedPersons.add(personId)
            newEvidence.leastRecentlyUsedList += IdTimeTuple(personId, timeMillis)
            newEvidence.counter = 1
        } else {
            val existingEvidence = evidenceMap[shownAttribute]!!

            if (!existingEvidence.witnessedPersons.contains(personId)) {
                existingEvidence.witnessedPersons.add(personId)
                existingEvidence.counter++
                existingEvidence.leastRecentlyUsedList += IdTimeTuple(personId, timeMillis)
            } else {
                existingEvidence.leastRecentlyUsedList.removeIf { it.first == personId }
                existingEvidence.leastRecentlyUsedList += IdTimeTuple(personId, timeMillis)
            }
        }

        val attributeEvidence = evidenceMap[shownAttribute]!!

        val toRemove = attributeEvidence.leastRecentlyUsedList.filter { it.second < timeMillis - deltaMillis }
        toRemove.forEach {
            attributeEvidence.leastRecentlyUsedList.remove(it)
            attributeEvidence.witnessedPersons.remove(it.first)
            attributeEvidence.counter--
        }

        return attributeEvidence.counter >= z
    }
}

typealias ShownAttribute = Pair<String, Any>