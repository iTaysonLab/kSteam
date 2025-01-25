package bruhcollective.itaysonlab.ksteam.database.room.entity.persona

import androidx.room.Embedded
import androidx.room.Relation
import bruhcollective.itaysonlab.ksteam.database.room.entity.persona.RoomPersona.DatabaseStatusTypes
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.enums.EFriendRelationship
import bruhcollective.itaysonlab.ksteam.models.persona.AvatarHash
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import bruhcollective.itaysonlab.ksteam.models.persona.Persona.Status.InGame
import bruhcollective.itaysonlab.ksteam.models.toSteamId

internal data class RoomFullPersona(
    @Embedded
    val persona: RoomPersona,

    @Relation(
        parentColumn = "uid",
        entityColumn = "uid",
    )
    val relationship: RoomPersonaRelationship,

    @Relation(
        parentColumn = "uid",
        entityColumn = "uid",
        entity = RoomPersonaRpKvo::class
    )
    val richPresenceKvo: List<RoomPersonaRpKvo>
) {
    fun convert(): Persona {
        return Persona(
            id = persona.uid.toSteamId(),
            name = persona.name,
            avatar = AvatarHash(persona.avatarHash),
            lastSeen = Persona.LastSeen(
                lastLogOn = persona.lastSeenLogOn,
                lastLogOff = persona.lastSeenLogOff,
                lastSeenOnline = persona.lastSeenLogOnline
            ),
            status = convertToStatus(),
            relationship = EFriendRelationship.byEncoded(relationship.relationship)
        )
    }

    private fun convertToStatus(): Persona.Status {
        return when (persona.status) {
            DatabaseStatusTypes.CACHE_TYPE_DEFAULT -> {
                if (persona.statusOnlineAdditional == 0) {
                    Persona.Status.Offline
                } else {
                    Persona.Status.Online(additional = Persona.OnlineStatus.entries[persona.statusOnlineAdditional ?: 0])
                }
            }

            DatabaseStatusTypes.CACHE_TYPE_IN_NON_STEAM_GAME -> {
                Persona.Status.InNonSteamGame(name = persona.statusNonSteamName.orEmpty())
            }

            DatabaseStatusTypes.CACHE_TYPE_IN_GAME -> {
                val rp = richPresenceKvo.associate { it.key to it.value }

                InGame(
                    appId = AppId(persona.statusSteamAppid ?: 0),
                    playerGroupId = rp["steam_player_group"],
                    playerGroupSize = rp["steam_player_group_size"]?.toInt() ?: 0,
                    displayText = null, // This should be dispatched later
                    richPresence = rp
                )
            }

            else -> Persona.Status.Offline
        }
    }
}