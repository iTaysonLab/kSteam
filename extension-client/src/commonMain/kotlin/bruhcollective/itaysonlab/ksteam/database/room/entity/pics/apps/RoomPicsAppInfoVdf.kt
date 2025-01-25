package bruhcollective.itaysonlab.ksteam.database.room.entity.pics.apps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import bruhcollective.itaysonlab.ksteam.database.room.TableNames

@Entity(
    tableName = TableNames.APP_INFO_DATA,
    foreignKeys = [
        ForeignKey(
            entity = RoomPicsAppInfo::class,
            parentColumns = ["id"],
            childColumns = ["app_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
internal data class RoomPicsAppInfoVdf(
    @PrimaryKey @ColumnInfo("app_id") val appid: Int,
    @ColumnInfo("data") val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RoomPicsAppInfoVdf

        if (appid != other.appid) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = appid
        result = 31 * result + data.contentHashCode()
        return result
    }
}