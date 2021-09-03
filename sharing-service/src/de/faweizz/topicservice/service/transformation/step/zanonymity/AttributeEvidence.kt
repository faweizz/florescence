/*
Implementation of algorithm proposed in https://ieeexplore.ieee.org/document/9378422
 */

package de.faweizz.topicservice.service.transformation.step.zanonymity

data class AttributeEvidence(
    var counter: Long = 0,
    val leastRecentlyUsedList: MutableList<IdTimeTuple> = mutableListOf(),
    val witnessedPersons: HashSet<String> = HashSet()
)

typealias IdTimeTuple = Pair<String, Long>