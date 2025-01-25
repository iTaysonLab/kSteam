package bruhcollective.itaysonlab.ksteam.database.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.RoomPicsAppEntry
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.RoomPicsPackageEntry

@Dao
internal interface RoomPicsEntryDao {
    @Query("SELECT * FROM entry_package WHERE id = :id")
    suspend fun getPackageEntryById(id: Int): RoomPicsPackageEntry?

    @Query("SELECT * FROM entry_app WHERE id = :id")
    suspend fun getAppEntryById(id: Int): RoomPicsAppEntry?

    @Upsert
    suspend fun upsertPackageEntry(entry: RoomPicsPackageEntry)

    @Upsert
    suspend fun upsertAppEntry(entry: RoomPicsAppEntry)

    @Query("SELECT MAX(change_number) FROM entry_package")
    suspend fun getPackageLastChangeNumber(): Int

    @Query("SELECT MAX(change_number) FROM entry_app")
    suspend fun getAppLastChangeNumber(): Int

    @Query("SELECT access_token FROM entry_package WHERE id = :id")
    suspend fun getAccessTokenForPackage(id: Int): Long?

    @Query("SELECT access_token FROM entry_app WHERE id = :id")
    suspend fun getAccessTokenForApp(id: Int): Long?
}