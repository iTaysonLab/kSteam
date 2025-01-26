package bruhcollective.itaysonlab.ksteam.database.room.entity.pics.apps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import bruhcollective.itaysonlab.ksteam.database.room.TableNames

@Entity(
    tableName = TableNames.APP_INFO_DESCRIPTORS,
    primaryKeys = ["app_id", "descriptor"],
    indices = [Index("app_id"), Index("descriptor")],
    foreignKeys = [
        ForeignKey(
            entity = RoomPicsAppInfo::class,
            parentColumns = ["id"],
            childColumns = ["app_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
internal data class RoomPicsAppInfoContentDescriptor(
    @ColumnInfo("app_id") val appId: Int,
    @ColumnInfo("descriptor") val descriptor: Int
)