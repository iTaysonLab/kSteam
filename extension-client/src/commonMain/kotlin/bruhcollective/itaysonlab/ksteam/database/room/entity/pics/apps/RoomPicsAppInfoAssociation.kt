package bruhcollective.itaysonlab.ksteam.database.room.entity.pics.apps

import androidx.room.*
import bruhcollective.itaysonlab.ksteam.database.room.TableNames

@Entity(
    tableName = TableNames.APP_INFO_ASSOCIATIONS,
    indices = [Index("app_id"), Index("type")],
    foreignKeys = [
        ForeignKey(
            entity = RoomPicsAppInfo::class,
            parentColumns = ["id"],
            childColumns = ["app_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
internal data class RoomPicsAppInfoAssociation(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("id") val internalId: Long = 0,
    @ColumnInfo("app_id") val appId: Int,
    @ColumnInfo("type") val type: String,
    @ColumnInfo("name") val name: String,
)