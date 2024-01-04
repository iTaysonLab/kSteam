package bruhcollective.itaysonlab.ksteam.database.models.persona

import bruhcollective.itaysonlab.ksteam.database.models.ConvertsTo
import bruhcollective.itaysonlab.ksteam.models.enums.EPersonaState
import bruhcollective.itaysonlab.ksteam.models.persona.AvatarHash
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import bruhcollective.itaysonlab.ksteam.models.toSteamId
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

internal class RealmPersona(): RealmObject, ConvertsTo<Persona> {
    @PrimaryKey var id: Long = 0L
    var name: String = ""
    var avatarHash: String = ""
    var lastSeen: RealmPersonaLastSeen? = null
    var onlineStatus: Int = EPersonaState.Offline.ordinal
    var ingame: RealmPersonaIngameStatus? = null

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