package bruhcollective.itaysonlab.ksteam.database.room.entity.apps

import androidx.room.ColumnInfo
import androidx.room.Entity
import bruhcollective.itaysonlab.ksteam.database.room.TableNames

@Entity(tableName = TableNames.RICH_PRESENCE_DICTIONARY, primaryKeys = ["appid", "lang"])
internal data class RoomRichPresenceDictionary(
    @ColumnInfo("appid") val appId: Int,
    @ColumnInfo("lang") val language: String,
    @ColumnInfo("expires_at") val expiresAt: Long,

    // CCommunity_GetAppRichPresenceLocalization_Response_TokenList
    @ColumnInfo("data") val content: ByteArray,
)