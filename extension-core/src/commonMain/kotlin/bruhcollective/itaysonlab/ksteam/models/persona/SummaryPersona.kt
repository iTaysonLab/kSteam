package bruhcollective.itaysonlab.ksteam.models.persona

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EPersonaState

/**
 * A persona is a user in the Steam network.
 */
data class SummaryPersona internal constructor(
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
    val lastSeen: Persona.LastSeen,
    /**
     * Online type
     */
    val onlineStatus: EPersonaState,
    /**
     * The URL to a profile on the Internet
     */
    val profileUrl: String,
) {
    internal constructor(obj: PlayerSummary) : this(
        id = SteamId(obj.steamid.toULong()),
        name = obj.personaname,
        avatar = AvatarHash(obj.avatarhash.orEmpty()),
        lastSeen = Persona.LastSeen(
            lastLogOff = obj.lastlogoff ?: 0,
            lastLogOn = obj.lastlogon ?: 0,
            lastSeenOnline = obj.lastseenonline ?: 0
        ),
        onlineStatus = EPersonaState.byEncoded(obj.personastate),
        profileUrl = obj.profileurl
    )
}