package bruhcollective.itaysonlab.ksteam.database.room.entity.pics.packages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import bruhcollective.itaysonlab.ksteam.database.room.TableNames

// appIds/depotIds are in separate tables
@Entity(
    tableName = TableNames.PACKAGE_INFO_APPS,
    primaryKeys = ["id", "app_id"],
    indices = [Index(value = ["id"]), Index(value = ["app_id"])],
    foreignKeys = [
        ForeignKey(
            entity = RoomPicsPackageInfo::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
internal data class RoomPicsPackageInfoGrantedApp(
    @ColumnInfo("id") val id: Int,
    @ColumnInfo("app_id") val appId: Int,
)