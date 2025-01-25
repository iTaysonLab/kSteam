package bruhcollective.itaysonlab.ksteam.database.room.entity.store

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import bruhcollective.itaysonlab.ksteam.database.room.TableNames

@Entity(tableName = TableNames.STORE_TAGS, primaryKeys = ["id", "lang"], indices = [Index("id")])
internal data class RoomStoreTag (
    @ColumnInfo("id")
    val id: Int,

    @ColumnInfo(name = "lang")
    val language: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "normalized")
    val normalizedName: String,
)