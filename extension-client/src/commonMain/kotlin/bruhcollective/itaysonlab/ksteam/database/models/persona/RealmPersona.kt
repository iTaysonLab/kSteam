package bruhcollective.itaysonlab.ksteam.database.models.persona

import bruhcollective.itaysonlab.ksteam.database.models.ConvertsTo
import bruhcollective.itaysonlab.ksteam.database.models.MergesWith
import bruhcollective.itaysonlab.ksteam.models.enums.EFriendRelationship
import bruhcollective.itaysonlab.ksteam.models.persona.AvatarHash
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import bruhcollective.itaysonlab.ksteam.models.toSteamId
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

internal class RealmPersona(): RealmObject, ConvertsTo<Persona>, MergesWith<Persona> {
    @PrimaryKey var id: Long = 0L
    var name: String = ""
    var avatarHash: String = ""
    var lastSeen: RealmPersonaLastSeen? = null
    var status: RealmPersonaStatus? = null
    var relationship: Int = 0

    // id: SteamId, name: String, avatar: AvatarHash, lastSeen: LastSeen, status: Status
    constructor(ks: Persona): this() {
        id = ks.id.longId
        merge(ks)
    }

    override fun merge(with: Persona) {
        name = with.name
        avatarHash = with.avatar.hash
        lastSeen = RealmPersonaLastSeen(with.lastSeen)
        status = RealmPersonaStatus(with.status)
    }

    override fun convert(): Persona {
        return Persona(
            id = id.toSteamId(),
            name = name,
            avatar = AvatarHash(avatarHash),
            lastSeen = lastSeen?.convert() ?: Persona.LastSeen(),
            status = status?.convert() ?: Persona.Status.Offline,
            relationship = EFriendRelationship.byEncoded(relationship)
        )
    }
}