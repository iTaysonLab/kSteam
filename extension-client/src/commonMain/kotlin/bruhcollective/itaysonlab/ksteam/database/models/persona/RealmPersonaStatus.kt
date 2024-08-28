package bruhcollective.itaysonlab.ksteam.database.models.persona

import bruhcollective.itaysonlab.ksteam.database.models.ConvertsTo
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import io.realm.kotlin.ext.realmDictionaryOf
import io.realm.kotlin.ext.toRealmDictionary
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmDictionary

internal class RealmPersonaStatus(): EmbeddedRealmObject, ConvertsTo<Persona.Status> {
    // = [Type Enum], 0 = Offline, 1 = Online, 2 = InNonSteamGame, 3 = InGame
    var type: Int = RealmPersonaStatusTypes.CACHE_TYPE_OFFLINE

    // = [Status.Online]
    var onlineAdditional: Int = 0

    // = [Status.InNonSteamGame]
    var nonSteamName: String? = null

    // = [Status.InGame]
    var steamAppid: Int = 0
    var steamRichPresence: RealmDictionary<String> = realmDictionaryOf()
    var steamPlayerGroupId: String? = null
    var steamPlayerGroupSize: Int = 0
    var steamDisplayText: String? = null

    constructor(ks: Persona.Status): this() {
        when (ks) {
            Persona.Status.Offline -> {
                type = RealmPersonaStatusTypes.CACHE_TYPE_OFFLINE
            }

            is Persona.Status.Online -> {
                type = RealmPersonaStatusTypes.CACHE_TYPE_ONLINE
                onlineAdditional = ks.additional.ordinal
            }

            is Persona.Status.InNonSteamGame -> {
                type = RealmPersonaStatusTypes.CACHE_TYPE_IN_NON_STEAM_GAME
                nonSteamName = ks.name
            }

            is Persona.Status.InGame -> {
                type = RealmPersonaStatusTypes.CACHE_TYPE_IN_GAME
                steamAppid = ks.appId.value
                steamPlayerGroupId = ks.playerGroupId
                steamPlayerGroupSize = ks.playerGroupSize
                steamDisplayText = ks.displayText
                steamRichPresence = ks.richPresence.toRealmDictionary()
            }
        }
    }

    override fun convert(): Persona.Status {
        return when (type) {
            RealmPersonaStatusTypes.CACHE_TYPE_ONLINE -> {
                Persona.Status.Online(additional = Persona.OnlineStatus.entries[onlineAdditional])
            }

            RealmPersonaStatusTypes.CACHE_TYPE_IN_NON_STEAM_GAME -> {
                Persona.Status.InNonSteamGame(name = nonSteamName.orEmpty())
            }

            RealmPersonaStatusTypes.CACHE_TYPE_IN_GAME -> {
                Persona.Status.InGame(
                    appId = AppId(steamAppid),
                    playerGroupId = steamPlayerGroupId,
                    playerGroupSize = steamPlayerGroupSize,
                    displayText = steamDisplayText.orEmpty(),
                    richPresence = steamRichPresence.toMap()
                )
            }

            else -> Persona.Status.Offline
        }
    }
}

private object RealmPersonaStatusTypes {
    const val CACHE_TYPE_OFFLINE = 0
    const val CACHE_TYPE_ONLINE = 1
    const val CACHE_TYPE_IN_NON_STEAM_GAME = 2
    const val CACHE_TYPE_IN_GAME = 3
}