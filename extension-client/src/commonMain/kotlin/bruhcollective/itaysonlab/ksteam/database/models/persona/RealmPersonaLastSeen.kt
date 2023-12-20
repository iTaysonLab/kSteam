package bruhcollective.itaysonlab.ksteam.database.models.persona

import bruhcollective.itaysonlab.ksteam.database.models.ConvertsTo
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import io.realm.kotlin.types.EmbeddedRealmObject

internal class RealmPersonaLastSeen(): EmbeddedRealmObject, ConvertsTo<Persona.LastSeen> {
    var lastLogOn: Int = 0
    var lastLogOff: Int = 0
    var lastSeenOnline: Int = 0

    constructor(ks: Persona.LastSeen): this() {
        lastLogOn = ks.lastLogOn
        lastLogOff = ks.lastLogOff
        lastSeenOnline = ks.lastSeenOnline
    }

    override fun convert(): Persona.LastSeen = Persona.LastSeen(lastLogOn, lastLogOff, lastSeenOnline)
}