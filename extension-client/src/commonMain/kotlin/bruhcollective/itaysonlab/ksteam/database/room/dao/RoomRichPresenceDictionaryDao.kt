package bruhcollective.itaysonlab.ksteam.database.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import bruhcollective.itaysonlab.ksteam.database.room.entity.apps.RoomRichPresenceDictionary

@Dao
internal interface RoomRichPresenceDictionaryDao {
    @Query("SELECT * FROM rich_presence WHERE appid == :appId AND lang == :language")
    suspend fun get(appId: Int, language: String): RoomRichPresenceDictionary?

    @Upsert
    suspend fun upsert(dict: RoomRichPresenceDictionary)
}