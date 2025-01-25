package bruhcollective.itaysonlab.ksteam.database.room.entity.persona

import androidx.room.ColumnInfo
import androidx.room.Entity
import bruhcollective.itaysonlab.ksteam.database.room.TableNames

@Entity(tableName = TableNames.PERSONA_RP_KVO, primaryKeys = ["uid", "key"])
internal data class RoomPersonaRpKvo(
    @ColumnInfo("uid") val uid: Long,
    @ColumnInfo("key") val key: String,
    @ColumnInfo("value") val value: String,
)