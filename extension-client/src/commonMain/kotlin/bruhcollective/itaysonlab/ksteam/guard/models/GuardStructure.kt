package bruhcollective.itaysonlab.ksteam.guard.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GuardStructure (
    @SerialName("shared_secret") val sharedSecret: String, // Required, Base64
    @SerialName("serial_number") val serialNumber: Long = 0L,
    @SerialName("revocation_code") val revocationCode: String = "",
    @SerialName("uri") val uri: String = "",
    @SerialName("server_time") val serverTime: Long = 0L,
    @SerialName("account_name") val accountName: String = "",
    @SerialName("token_gid") val tokenGid: String = "",
    @SerialName("identity_secret") val identitySecret: String, // Required, Base64
    @SerialName("secret_1") val secretOne: String = "", // Base64
    @SerialName("steam_id") val steamId: Long = 0, // Optional, kSteam exclusive
)