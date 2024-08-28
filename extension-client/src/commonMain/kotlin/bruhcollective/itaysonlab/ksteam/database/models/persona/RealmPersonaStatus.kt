package bruhcollective.itaysonlab.ksteam.database.models.persona

import bruhcollective.itaysonlab.ksteam.database.models.ConvertsTo
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import io.realm.kotlin.ext.realmDictionaryOf
import io.realm.kotlin.ext.toRealmDictionary
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmDictionary

internal class RealmPersonaIngameStatus(): EmbeddedRealmObject, ConvertsTo<Persona.IngameStatus> {
    // = [Type Enum], 0 = None, 1 = Steam, 2 = NonSteam
    var type: Int = 0

    // = [IngameStatus.Steam]
    var steamAppid: Int = 0
    var steamRichPresence: RealmDictionary<String> = realmDictionaryOf()

    // = [IngameStatus.NonSteam]
    var nonSteamName: String = ""

    constructor(ks: Persona.IngameStatus): this() {
        when (ks) {
            Persona.IngameStatus.None -> {
                type = 0
            }

            is Persona.IngameStatus.Steam -> {
                type = 1
                steamAppid = ks.appId
                steamRichPresence = ks.richPresence.toRealmDictionary()
            }

            is Persona.IngameStatus.NonSteam -> {
                type = 2
                nonSteamName = ks.name
            }
        }
    }

    override fun convert(): Persona.IngameStatus {
        return when (type) {
            1 -> Persona.IngameStatus.Steam(
                appId = steamAppid,
                richPresence = steamRichPresence
            )

            2 -> Persona.IngameStatus.NonSteam(
                name = nonSteamName
            )

            else -> Persona.IngameStatus.None
        }
    }
}