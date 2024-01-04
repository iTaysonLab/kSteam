package bruhcollective.itaysonlab.ksteam.database.models.persona

import bruhcollective.itaysonlab.ksteam.database.models.ConvertsTo
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EFriendRelationship
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

internal class RealmPersonaRelationship(): RealmObject, ConvertsTo<EFriendRelationship> {
    // ID of format {SRC}_{TARGET}, so for kSteam user 0 relationship for user 1 will be "0_1"
    @PrimaryKey var id: String = ""
    var relationship: Int = 0

    constructor(src: SteamId, target: SteamId, enum: Int): this() {
        id = "${src}_${target}"
        relationship = enum
    }

    override fun convert(): EFriendRelationship {
        return EFriendRelationship.byEncoded(relationship)
    }
}