package bruhcollective.itaysonlab.ksteam.database.room.entity.pics.apps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import bruhcollective.itaysonlab.ksteam.database.room.TableNames

@Entity(
    tableName = TableNames.APP_INFO_TAGS,
    primaryKeys = ["app_id", "tag_id"],
    indices = [Index("app_id"), Index("tag_id")],
    foreignKeys = [
        ForeignKey(
            entity = RoomPicsAppInfo::class,
            parentColumns = ["id"],
            childColumns = ["app_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
internal data class RoomPicsAppInfoStoreTagJunction(
    @ColumnInfo("app_id") val appId: Int,
    @ColumnInfo("tag_id") val tagId: Int
)