package bruhcollective.itaysonlab.ksteam.database.room

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import bruhcollective.itaysonlab.ksteam.database.room.dao.RoomPackageLicenseDao
import bruhcollective.itaysonlab.ksteam.database.room.dao.RoomPersonaDao
import bruhcollective.itaysonlab.ksteam.database.room.entity.apps.RoomPackageLicense
import bruhcollective.itaysonlab.ksteam.database.room.entity.persona.RoomPersona
import bruhcollective.itaysonlab.ksteam.database.room.entity.persona.RoomPersonaRelationship
import bruhcollective.itaysonlab.ksteam.database.room.entity.persona.RoomPersonaRpKvo

/**
 * A Room database for user-specific data like:
 * - package licenses
 * - persona information
 */
@Database(
    entities = [
        RoomPackageLicense::class,
        RoomPersona::class,
        RoomPersonaRelationship::class,
        RoomPersonaRpKvo::class,
    ],
    version = 1
)
@ConstructedBy(KsUserDatabaseInitializer::class)
internal abstract class KsUserDatabase: RoomDatabase() {
    companion object {
        fun newInstance(builder: Builder<KsUserDatabase>): KsUserDatabase {
            return builder
                .fallbackToDestructiveMigration(true) // TODO: disable when schema stabilizes
                .setDriver(BundledSQLiteDriver())
                .build()
        }
    }

    abstract fun packageLicenses(): RoomPackageLicenseDao
    abstract fun personas(): RoomPersonaDao
}

internal expect object KsUserDatabaseInitializer: RoomDatabaseConstructor<KsUserDatabase>