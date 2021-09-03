package de.faweizz.topicservice.adjacent.keycloak

data class CreatedClient(
    val name: String,
    val secret: String,
    val consumerGroup: String
)
