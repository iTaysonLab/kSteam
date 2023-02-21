package bruhcollective.itaysonlab.ksteam.models.persona

import androidx.compose.runtime.Stable
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

) {
    companion object {
        val Unknown = Persona(
            id = SteamId.Empty,
            name = "",
            avatar = AvatarHash(""),
            lastSeen = LastSeen(0, 0, 0),
            onlineStatus = EPersonaState.Offline
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
        onlineStatus = EPersonaState.byEncoded(obj.persona_state ?: 0)
    )

    internal constructor(obj: PlayerSummary) : this(
        id = SteamId(obj.steamid.toULong()),
        name = obj.personaname,
        avatar = AvatarHash(obj.avatarhash.orEmpty()),
        lastSeen = LastSeen(
            lastLogOff = obj.lastlogoff ?: 0,
            lastLogOn = obj.lastlogon ?: 0,
            lastSeenOnline = obj.lastseenonline ?: 0
        ),
        onlineStatus = EPersonaState.byEncoded(obj.personastate)
    )

    @Stable
    data class LastSeen internal constructor(
        val lastLogOn: Int,
        val lastLogOff: Int,
        val lastSeenOnline: Int
    )
}