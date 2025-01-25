package bruhcollective.itaysonlab.ksteam.database

import androidx.room.RoomDatabase
import bruhcollective.itaysonlab.ksteam.database.room.KsSharedDatabase
import bruhcollective.itaysonlab.ksteam.database.room.KsUserDatabase
import bruhcollective.itaysonlab.ksteam.models.SteamId

/**
 * A platform-dependant class that provides Room database builders.
 */
expect class KSteamDatabaseProvider {
    internal fun createSharedDatabase(): RoomDatabase.Builder<KsSharedDatabase>
    internal fun createUserDatabase(id: SteamId): RoomDatabase.Builder<KsUserDatabase>
}