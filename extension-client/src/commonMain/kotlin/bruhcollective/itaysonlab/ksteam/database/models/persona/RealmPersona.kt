package bruhcollective.itaysonlab.ksteam.database.models.persona

import bruhcollective.itaysonlab.ksteam.database.models.ConvertsTo
import bruhcollective.itaysonlab.ksteam.models.enums.EPersonaState
import bruhcollective.itaysonlab.ksteam.models.persona.AvatarHash
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import bruhcollective.itaysonlab.ksteam.models.toSteamId
import io.realm.kotlin.ext.realmDictionaryOf
import io.realm.kotlin.types.RealmDictionary
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

internal class RealmPersona(): RealmObject, ConvertsTo<Persona> {
    @PrimaryKey var id: Long = 0L
    var name: String = ""
    var avatarHash: String = ""
    var lastSeen: RealmPersonaLastSeen? = null
    var onlineStatus: Int = EPersonaState.Offline.ordinal
    var ingame: RealmPersonaIngameStatus? = null

    /**
     * Relationships (with the kSteam users)
     *
     * For example, if a RealmPerson with id 1 is currently friends with the user 2 (and the user 2 is currently signed in to kSteam and fetched this information), this variable will look like this:
     * ["2": 3]
     *
     * The same applies to 2+ users.
     * So, to find friends of user "2" we need to query RealmPersons with "ANY friendRelationshipsWith['2'] == 3"
     *
     * **NOTE**: Don't blind-copy new RealmPersona objects!
     * This can lead to losing this property as it doesn't convert to kSteam persona.
     * Instead, you can edit the RealmPersona in a MutableRealm.
     */
    var friendRelationshipsWith: RealmDictionary<Int> = realmDictionaryOf()

    constructor(ks: Persona): this() {
        id = ks.id.longId
        name = ks.name
        avatarHash = ks.avatar.hash
        lastSeen = RealmPersonaLastSeen(ks.lastSeen)
        onlineStatus = ks.onlineStatus.ordinal
        ingame = RealmPersonaIngameStatus(ks.ingame)
    }

    override fun convert(): Persona {
        return Persona(
            id = id.toSteamId(),
            name = name,
            avatar = AvatarHash(avatarHash),
            lastSeen = lastSeen?.convert() ?: Persona.LastSeen(),
            onlineStatus = EPersonaState.byEncoded(onlineStatus),
            ingame = ingame?.convert() ?: Persona.IngameStatus.None
        )
    }
}