package bruhcollective.itaysonlab.ksteam.database

import bruhcollective.itaysonlab.ksteam.database.models.persona.RealmPersona
import bruhcollective.itaysonlab.ksteam.database.models.persona.RealmPersonaIngameStatus
import bruhcollective.itaysonlab.ksteam.database.models.persona.RealmPersonaLastSeen
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.types.TypedRealmObject
import okio.Path
import kotlin.reflect.KClass

/**
 * Manages Realm database for kSteam Client Extension.
 *
 * kSteam manages its separate realm in a file "ksteam.realm" inside working directory to ensure that the library won't collide with other Realm-using libraries or apps.
 */
internal class KSteamRealmDatabase (
    workingDirectory: Path
) {
    private companion object {
        const val FILE_NAME = "ksteam.realm"
        const val SCHEMA_VERSION = 1L
    }

    internal val realm = Realm.open(
        RealmConfiguration.Builder(createRealmSchema())
            .directory(workingDirectory.toString())
            .name(FILE_NAME)
            .schemaVersion(SCHEMA_VERSION)
            .build()
    )

    private fun createRealmSchema(): Set<KClass<out TypedRealmObject>> = setOf(
        // Persona
        RealmPersona::class,
        RealmPersonaIngameStatus::class,
        RealmPersonaLastSeen::class,
        // Store

        // PICS

        // Library
    )
}