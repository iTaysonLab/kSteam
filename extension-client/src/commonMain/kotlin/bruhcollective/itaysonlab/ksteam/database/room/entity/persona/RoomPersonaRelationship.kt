package bruhcollective.itaysonlab.ksteam.database.room.entity.persona

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import bruhcollective.itaysonlab.ksteam.database.room.TableNames

@Entity(tableName = TableNames.PERSONA_RELATIONSHIP)
internal data class RoomPersonaRelationship(
    @PrimaryKey @ColumnInfo("uid") val uid: Long,
    @ColumnInfo("relationship") val relationship: Int,
)