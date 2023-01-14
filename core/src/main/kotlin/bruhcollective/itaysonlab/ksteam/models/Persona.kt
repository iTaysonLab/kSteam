package bruhcollective.itaysonlab.ksteam.models

import steam.messages.clientserver_friends.CMsgClientPersonaState

/**
 * A persona is a user in the Steam network.
 *
 * Some of the data can be defined as "unknown" - you need to call specific methods in [bruhcollective.itaysonlab.ksteam.handlers.Persona] to request that data.
 */
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
) {
    internal constructor(obj: CMsgClientPersonaState.Friend): this(
        id = SteamId(obj.friendid?.toULong() ?: 0u),
        name = obj.player_name.orEmpty(),
        avatar = AvatarHash(obj.avatar_hash?.hex() ?: ""),
        lastSeen = LastSeen(
            lastLogOff = obj.last_logoff ?: 0,
            lastLogOn = obj.last_logon ?: 0,
            lastSeenOnline = obj.last_seen_online ?: 0
        )
    )

    data class LastSeen internal constructor(
        val lastLogOn: Int,
        val lastLogOff: Int,
        val lastSeenOnline: Int
    )
}