package bruhcollective.itaysonlab.ksteam.models.game_notes

import bruhcollective.itaysonlab.ksteam.models.AppId
import com.squareup.wire.WireField
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class GameNote (
    val id: String,
    val appid: AppId,
    val shortcutName: String?,
    val shortcutId: Int?,
    val ordinal: Int?,
    val timeCreated: Instant?,
    val timeModified: Instant?,
    val title: String,
    val content: String,
)