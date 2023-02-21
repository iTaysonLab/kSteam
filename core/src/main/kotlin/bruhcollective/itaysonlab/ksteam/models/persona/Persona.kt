package bruhcollective.itaysonlab.ksteam.models.persona

import androidx.compose.runtime.Stable
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EPersonaState
import steam.webui.common.CMsgClientPersonaState_Friend

/**
 * A persona is a user in the Steam network.
 *
 * Some of the data can be defined as "unknown" - you need to call specific methods in [bruhcollective.itaysonlab.ksteam.handlers.Persona] to request that data.
 */
@Stable
data class Persona internal constructor(
    /**
     * The [SteamId] of the user.
     */
    val id: SteamId,
    /**
     * Username. This is not a Vanity URL, but rather a name that's displayed publicly.
     */
    val name: String,
    /**
     * Avatar hash. You would probably have no need to directly use this, so a wrapper is provided.
     */
    val avatar: AvatarHash,
    /**
     * Last seen information
     */
    val lastSeen: LastSeen,
    /**
     * Online type
     */
    val onlineStatus: EPersonaState,
    /**
     * If in game, this will be game's AppID.
     */
    val ingameAppId: AppId,
    /**
     * If in game, this will be game's rich presence information.
     */
    val ingameRichPresence: Map<String, String>
) {
    companion object {
        val Unknown = Persona(
            id = SteamId.Empty,
            name = "",
            avatar = AvatarHash(""),
            lastSeen = LastSeen(0, 0, 0),
            onlineStatus = EPersonaState.Offline,
            ingameAppId = AppId(0),
            ingameRichPresence = emptyMap()
        )
    }

    internal constructor(obj: CMsgClientPersonaState_Friend) : this(
        id = SteamId(obj.friendid?.toULong() ?: 0u),
        name = obj.player_name.orEmpty(),
        avatar = AvatarHash(obj.avatar_hash?.hex() ?: ""),
        lastSeen = LastSeen(
            lastLogOff = obj.last_logoff ?: 0,
            lastLogOn = obj.last_logon ?: 0,
            lastSeenOnline = obj.last_seen_online ?: 0
        ),
        onlineStatus = EPersonaState.byEncoded(obj.persona_state ?: 0),
        ingameAppId = AppId(obj.gameid?.toInt() ?: 0),
        ingameRichPresence = obj.rich_presence.associate { it.key.orEmpty() to it.value_.orEmpty() }
    )

    @Stable
    data class LastSeen internal constructor(
        val lastLogOn: Int,
        val lastLogOff: Int,
        val lastSeenOnline: Int
    )
}