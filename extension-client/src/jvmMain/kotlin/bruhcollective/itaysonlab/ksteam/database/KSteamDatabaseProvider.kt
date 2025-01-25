package bruhcollective.itaysonlab.ksteam.database

import androidx.room.Room
import androidx.room.RoomDatabase
import bruhcollective.itaysonlab.ksteam.database.room.KsSharedDatabase
import bruhcollective.itaysonlab.ksteam.database.room.KsSharedDatabase_Impl
import bruhcollective.itaysonlab.ksteam.database.room.KsUserDatabase
import bruhcollective.itaysonlab.ksteam.database.room.KsUserDatabase_Impl
import bruhcollective.itaysonlab.ksteam.models.SteamId

actual class KSteamDatabaseProvider() {
    internal actual fun createSharedDatabase(): RoomDatabase.Builder<KsSharedDatabase> {
        return Room.databaseBuilder<KsSharedDatabase>("ksteam/database/ks_shared.db", ::KsSharedDatabase_Impl)
    }

    internal actual fun createUserDatabase(id: SteamId): RoomDatabase.Builder<KsUserDatabase> {
        return Room.databaseBuilder<KsUserDatabase>("ksteam/database/ks_${id.id}.db", ::KsUserDatabase_Impl)
    }
}