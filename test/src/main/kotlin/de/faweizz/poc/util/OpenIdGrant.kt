package de.faweizz.poc.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class OpenIdGrant(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Long,
    @SerialName("refresh_expires_in")
    val refreshExpiresIn: Long,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("token_type")
    val tokenType: String,
    @SerialName("id_token")
    val idToken: String,
    @SerialName("not-before-policy")
    val notBeforePolicy: Long,
    val scope: String,
    @SerialName("session_state")
    val sessionState: String,
)