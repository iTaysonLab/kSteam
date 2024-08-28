package bruhcollective.itaysonlab.ksteam.models.account

data class SteamAccountAuthorization (
    val accountName: String,
    val accessToken: String,
    val refreshToken: String
)