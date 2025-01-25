package bruhcollective.itaysonlab.ksteam.database.room.entity.persona

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import bruhcollective.itaysonlab.ksteam.database.room.TableNames
import steam.webui.common.CMsgClientPersonaState_Friend

@Entity(tableName = TableNames.PERSONA)
internal data class RoomPersona(
    @PrimaryKey @ColumnInfo("uid") val uid: Long,
    @ColumnInfo("name") val name: String,
    @ColumnInfo("avatar_hash") val avatarHash: String,

    // Last Seen
    @ColumnInfo("last_seen_on") val lastSeenLogOn: Int,
    @ColumnInfo("last_seen_off") val lastSeenLogOff: Int,
    @ColumnInfo("last_seen_online") val lastSeenLogOnline: Int,

    // Status
    @ColumnInfo("status") val status: Int,
    // = [Status.Online]
    @ColumnInfo("status_online") val statusOnlineAdditional: Int?,
    // = [Status.InNonSteamGame]
    @ColumnInfo("status_non_steam_name") val statusNonSteamName: String?,
    // = [Status.InGame]
    @ColumnInfo("status_steam_appid") val statusSteamAppid: Int?,
) {
    internal object DatabaseStatusTypes {
        const val CACHE_TYPE_DEFAULT = 0
        const val CACHE_TYPE_IN_NON_STEAM_GAME = 1
        const val CACHE_TYPE_IN_GAME = 2
    }

    companion object {
        fun fromProtobufPersona(proto: CMsgClientPersonaState_Friend): RoomPersona {
            return RoomPersona(
                uid = proto.friendid!!,
                name = proto.player_name.orEmpty(),
                avatarHash = proto.avatar_hash?.hex().orEmpty(),
                //
                lastSeenLogOn = proto.last_logon ?: 0,
                lastSeenLogOff = proto.last_logoff ?: 0,
                lastSeenLogOnline = proto.last_seen_online ?: 0,
                //
                status = when {
                    proto.game_name.isNullOrEmpty().not() -> DatabaseStatusTypes.CACHE_TYPE_IN_NON_STEAM_GAME
                    proto.gameid != null && proto.gameid!! > 0L -> DatabaseStatusTypes.CACHE_TYPE_IN_GAME
                    else -> DatabaseStatusTypes.CACHE_TYPE_DEFAULT
                },
                statusOnlineAdditional = proto.persona_state ?: 0,
                statusNonSteamName = proto.game_name,
                statusSteamAppid = proto.gameid?.toInt()
            )
        }
    }
}