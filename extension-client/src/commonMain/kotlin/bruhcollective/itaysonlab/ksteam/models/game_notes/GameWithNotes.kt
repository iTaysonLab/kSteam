package bruhcollective.itaysonlab.ksteam.models.game_notes

import bruhcollective.itaysonlab.ksteam.models.AppId
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class GameWithNotes (
    val appId: AppId,
    val shortcutName: String?,
    val shortcutId: Int?,
    val timeModified: Instant?,
    val noteCount: Int,
)