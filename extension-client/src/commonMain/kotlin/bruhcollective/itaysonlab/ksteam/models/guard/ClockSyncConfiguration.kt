package bruhcollective.itaysonlab.ksteam.models.guard

import kotlinx.serialization.Serializable

@Serializable
data class ClockSyncConfiguration (
    val clockSyncTz: String = "",
    val clockSyncDiff: Long = 0,
)