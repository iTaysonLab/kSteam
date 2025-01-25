package bruhcollective.itaysonlab.ksteam.database.room.entity.pics.apps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import bruhcollective.itaysonlab.ksteam.database.room.TableNames

@Entity(
    tableName = TableNames.APP_INFO_CATEGORIES,
    primaryKeys = ["app_id", "category_id"],
    indices = [Index("app_id"), Index("category_id")],
    foreignKeys = [
        ForeignKey(
            entity = RoomPicsAppInfo::class,
            parentColumns = ["id"],
            childColumns = ["app_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
internal data class RoomPicsAppInfoCategory(
    @ColumnInfo("app_id") val appId: Int,
    @ColumnInfo("category_id") val categoryId: Int
)