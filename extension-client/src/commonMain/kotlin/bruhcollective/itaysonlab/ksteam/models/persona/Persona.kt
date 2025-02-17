package bruhcollective.itaysonlab.ksteam.models.persona

import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EFriendRelationship
import bruhcollective.itaysonlab.ksteam.models.enums.EPersonaState
import bruhcollective.itaysonlab.ksteam.models.persona.Persona.Status.Offline
import bruhcollective.itaysonlab.ksteam.models.persona.Persona.Status.Online
import steam.webui.common.CMsgClientPersonaState_Friend

/**
 * A persona is a user in the Steam network.
 *
 * Some of the data can be defined as "unknown" - you need to call specific methods in [bruhcollective.itaysonlab.ksteam.handlers.Persona] to request that data.
 */
data class Persona (
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
    val avatar: AvatarHash = AvatarHash.Empty,

    /**
     * Last seen information
     */
    val lastSeen: LastSeen = LastSeen(),

    /**
     * Online status
     */
    val status: Status = Status.Offline,

    /**
     * Relationship of the current kSteam user to this user.
     */
    val relationship: EFriendRelationship = EFriendRelationship.None
) {
    val stringId get() = id.toString()

    data class LastSeen (
        val lastLogOn: Int = 0,
        val lastLogOff: Int = 0,
        val lastSeenOnline: Int = 0
    )

    enum class OnlineStatus {
        /**
         * The user is online.
         */
        Online,

        /**
         * The user is currently away.
         */
        Away,

        /**
         * The user is currently busy.
         */
        Busy,

        /**
         * The user is currently in "snooze" mode.
         */
        Snooze,

        /**
         * The user is currently in "looking to trade" mode.
         */
        LookingToTrade,

        /**
         * The user is currently in "looking to play" mode.
         */
        LookingToPlay
    }

    sealed interface Status {
        /**
         * The user is currently offline.
         */
        data object Offline: Status

        /**
         * The user is currently online, but not playing anything.
         */
        data class Online (
            /**
             * More detailed online message.
             */
            val additional: OnlineStatus
        ): Status

        /**
         * The user is currently online and playing a non-Steam game.
         */
        data class InNonSteamGame (
            /**
             * Known game name which the user is playing right now.
             */
            val name: String
        ): Status

        /**
         * The user is currently online and playing a Steam game.
         *
         * Extra data for rich presence is available, if provided by the game.
         */
        data class InGame (
            /**
             * The ID of a currently running game.
             */
            val appId: AppId,

            /**
             * If set up by the game, player group ID. If not, this variable will be null.
             *
             * This can be used to separate users in friend list.
             */
            val playerGroupId: String? = null,

            /**
             * If set up by the game, player group size. If not, this variable will be 0.
             *
             * This can be used to show "Playing with N other people"
             */
            val playerGroupSize: Int = 0,

            /**
             * If available, formatted rich presence data. If not, this variable will be null.
             *
             * This value is computed by the kSteam library.
             */
            val displayText: String? = null,

            /**
             * All rich presence data that were transmitted by the running game.
             */
            val richPresence: Map<String, String> = emptyMap()
        ): Status {
            /**
             * Returns if this player is a part of a group.
             */
            val isInsideAGroup: Boolean
                get() = playerGroupId != null
        }

        companion object {
            internal fun fromFriend(friend: CMsgClientPersonaState_Friend): Status {
                return when {
                    friend.game_name.isNullOrEmpty().not() -> {
                        InNonSteamGame(name = friend.game_name.orEmpty())
                    }

                    friend.gameid != null && friend.gameid!! > 0L -> {
                        val appId = friend.gameid!!
                        val richPresence = friend.rich_presence.associate { it.key.orEmpty() to it.value_.orEmpty() }

                        InGame(
                            appId = AppId(appId.toInt()),
                            playerGroupId = richPresence["steam_player_group"],
                            playerGroupSize = richPresence["steam_player_group_size"]?.toInt() ?: 0,
                            displayText = null, // This should be dispatched later
                            richPresence = richPresence
                        )
                    }

                    else -> ePersonaStateToStatus(friend.persona_state ?: 0)
                }
            }
        }
    }

    companion object {
        val Unknown = Persona(
            id = SteamId.Empty,
            name = ""
        )

        internal fun fromSummary(obj: PlayerSummary): Persona {
            return Persona(
                id = SteamId(obj.steamid.toULong()),
                name = obj.personaname,
                avatar = AvatarHash(obj.avatarhash.orEmpty()),
                lastSeen = LastSeen(
                    lastLogOff = obj.lastlogoff ?: 0,
                    lastLogOn = obj.lastlogon ?: 0,
                    lastSeenOnline = obj.lastseenonline ?: 0
                ),
                status = ePersonaStateToStatus(obj.personastate)
                // Request relationship from database?
            )
        }

        private fun ePersonaStateToStatus(state: Int): Status {
            return when (EPersonaState.byEncoded(state)) {
                EPersonaState.Offline -> Offline
                EPersonaState.Online -> Online(additional = OnlineStatus.Online)
                EPersonaState.Busy -> Online(additional = OnlineStatus.Busy)
                EPersonaState.Away -> Online(additional = OnlineStatus.Away)
                EPersonaState.Snooze -> Online(additional = OnlineStatus.Snooze)
                EPersonaState.LookingToTrade -> Online(additional = OnlineStatus.LookingToTrade)
                EPersonaState.LookingToPlay -> Online(additional = OnlineStatus.LookingToPlay)
            }
        }
    }
}