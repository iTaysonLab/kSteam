package bruhcollective.itaysonlab.ksteam.guard.models

import kotlinx.serialization.Serializable

@Serializable
data class ClockSyncConfiguration (
    val clockSyncTz: String = "",
    val clockSyncDiff: Long = 0,
)