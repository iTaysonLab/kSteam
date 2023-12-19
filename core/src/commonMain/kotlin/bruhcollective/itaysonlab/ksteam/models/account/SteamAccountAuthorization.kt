package bruhcollective.itaysonlab.ksteam.models.account

import kotlinx.serialization.Serializable

@Serializable
data class SteamAccountAuthorization (
    val accountName: String,
    val accessToken: String,
    val refreshToken: String
)