package bruhcollective.itaysonlab.ksteam.database.room.entity.pics

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import bruhcollective.itaysonlab.ksteam.database.room.TableNames

@Entity(tableName = TableNames.PICS_ENTRY_APP)
internal data class RoomPicsAppEntry (
    @PrimaryKey @ColumnInfo("id") val id: Int,
    @ColumnInfo("change_number") val changeNumber: Int,
    @ColumnInfo("access_token") val accessToken: Long,
)