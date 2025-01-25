package bruhcollective.itaysonlab.ksteam.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import bruhcollective.itaysonlab.ksteam.database.room.KsSharedDatabase
import bruhcollective.itaysonlab.ksteam.database.room.KsSharedDatabaseInitializer
import bruhcollective.itaysonlab.ksteam.database.room.KsUserDatabase
import bruhcollective.itaysonlab.ksteam.database.room.KsUserDatabaseInitializer
import bruhcollective.itaysonlab.ksteam.models.SteamId

actual class KSteamDatabaseProvider (
    private val applicationContext: Context,
) {
    internal actual fun createSharedDatabase(): RoomDatabase.Builder<KsSharedDatabase> {
        return Room.databaseBuilder<KsSharedDatabase>(applicationContext, "ks_shared.db", KsSharedDatabaseInitializer::initialize)
    }

    internal actual fun createUserDatabase(id: SteamId): RoomDatabase.Builder<KsUserDatabase> {
        return Room.databaseBuilder<KsUserDatabase>(applicationContext, "ks_${id.id}.db", KsUserDatabaseInitializer::initialize)
    }
}