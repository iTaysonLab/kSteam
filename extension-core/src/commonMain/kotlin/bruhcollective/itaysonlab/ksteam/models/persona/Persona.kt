package bruhcollective.itaysonlab.ksteam.models.persona

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EPersonaState
import bruhcollective.itaysonlab.ksteam.platform.Immutable
import steam.webui.common.CMsgClientPersonaState_Friend

/**
 * A persona is a user in the Steam network.
 *
 * Some of the data can be defined as "unknown" - you need to call specific methods in [bruhcollective.itaysonlab.ksteam.handlers.Persona] to request that data.
 */
@Immutable
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
     * In-game status
     */
    val ingame: IngameStatus
) {
    companion object {
        val Unknown = Persona(
            id = SteamId.Empty,
            name = "",
            avatar = AvatarHash(""),
            lastSeen = LastSeen(0, 0, 0),
            onlineStatus = EPersonaState.Offline,
            ingame = IngameStatus.None
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
        ingame = IngameStatus.fromFriend(obj)
    )

    data class LastSeen internal constructor(
        val lastLogOn: Int,
        val lastLogOff: Int,
        val lastSeenOnline: Int
    )

    sealed class IngameStatus {
        /**
         * The user is not currently in any game.
         */
        object None: IngameStatus()

        /**
         * The user is currently in a Steam game.
         */
        class Steam internal constructor(
            /**
             * The ID of a currently running game.
             */
            val appId: Int,

            /**
             * If available, there will be a rich presence data.
             */
            val richPresence: Map<String, String>
        ): IngameStatus()

        /**
         * The user is currently in a non-Steam game.
         */
        class NonSteam internal constructor(
            /**
             * Known game name which the user is playing right now.
             */
            val name: String
        ): IngameStatus()

        internal companion object {
            fun fromFriend(friend: CMsgClientPersonaState_Friend): IngameStatus {
                return when {
                    friend.game_name.isNullOrEmpty().not() -> {
                        NonSteam(name = friend.game_name.orEmpty())
                    }

                    friend.gameid != null && friend.gameid!! > 0L -> {
                        Steam(appId = friend.gameid?.toInt() ?: 0, richPresence = friend.rich_presence.associate { it.key.orEmpty() to it.value_.orEmpty() })
                    }

                    else -> None
                }
            }
        }
    }
}