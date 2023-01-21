package bruhcollective.itaysonlab.ksteam.models.storage

import kotlinx.serialization.Serializable

@Serializable
internal data class GlobalConfiguration (
    val availableAccounts: Map<ULong, SavedAccount> = emptyMap(),
    val defaultAccount: ULong = 0u,
    val machineId: String = "",
    val clockSyncTz: String = "",
    val clockSyncDiff: Long = 0,
)

@Serializable
internal data class SavedAccount(
    val steamId: ULong = 0u,
    val accountName: String = "",
    val lastKnownPersonaName: String = "",
    val lastKnownAvatar: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val sentryFileName: String = ""
)