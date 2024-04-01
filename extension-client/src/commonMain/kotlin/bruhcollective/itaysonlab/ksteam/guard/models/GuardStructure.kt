package bruhcollective.itaysonlab.ksteam.guard.models

import kotlinx.serialization.Serializable

/**
 * Intermediate structure representing saved SG metadata.
 */
@Serializable
data class GuardStructure (
    val sharedSecret: String, // Required, Base64
    val serialNumber: Long = 0L,
    val revocationCode: String = "",
    val uri: String = "",
    val serverTime: Long = 0L,
    val accountName: String = "",
    val tokenGid: String = "",
    val identitySecret: String, // Required, Base64
    val secretOne: String = "", // Base64
)