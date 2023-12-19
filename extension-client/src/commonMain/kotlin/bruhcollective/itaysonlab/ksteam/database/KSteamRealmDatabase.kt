package bruhcollective.itaysonlab.ksteam.database

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration

/**
 * Manages Realm database for kSteam Client Extension.
 *
 * kSteam manages its separate realm in a file "ksteam.realm" inside working directory to ensure that the library won't collide with other Realm-using libraries or apps.
 */
internal class KSteamRealmDatabase {
    private companion object {
        const val FILE_NAME = "ksteam.realm"
        const val SCHEMA_VERSION = 1L
    }

    private val realm = Realm.open(
        RealmConfiguration.Builder(createRealmSchema())
            .name(FILE_NAME)
            .schemaVersion(SCHEMA_VERSION)
            .build()
    )

    private fun createRealmSchema() = setOf(

    )
}