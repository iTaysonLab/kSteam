package bruhcollective.itaysonlab.ksteam.models.storage

import kotlinx.serialization.Serializable

@Serializable
data class GlobalConfiguration internal constructor(
    val availableAccounts: Map<ULong, SavedAccount> = emptyMap(),
    val defaultAccount: ULong = 0u,
    val machineId: String = "",
)

@Serializable
data class SavedAccount internal constructor(
    val steamId: ULong = 0u,
    val accountName: String = "",
    val lastKnownPersonaName: String = "",
    val lastKnownAvatar: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val sentryFileName: String = ""
)