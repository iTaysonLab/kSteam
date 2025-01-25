package bruhcollective.itaysonlab.ksteam.database.room.dao

import androidx.room.*
import bruhcollective.itaysonlab.ksteam.database.room.entity.store.RoomStoreTag

@Dao
internal interface RoomStoreTagDao {
    @Upsert
    suspend fun upsertStoreTag(entry: RoomStoreTag)

    @Upsert
    suspend fun upsertStoreTags(entry: List<RoomStoreTag>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnoreStoreTags(entry: List<RoomStoreTag>)

    @Query("SELECT * FROM store_tag WHERE id = :id")
    suspend fun getStoreTagById(id: Int): RoomStoreTag?
}