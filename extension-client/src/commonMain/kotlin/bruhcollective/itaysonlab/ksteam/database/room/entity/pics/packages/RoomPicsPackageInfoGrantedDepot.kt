package bruhcollective.itaysonlab.ksteam.database.room.entity.pics.packages

import androidx.room.ColumnInfo
import androidx.room.Entity
import bruhcollective.itaysonlab.ksteam.database.room.TableNames

// appIds/depotIds are in separate tables
@Entity(tableName = TableNames.PACKAGE_INFO_DEPOTS, primaryKeys = ["id", "depot_id"])
internal data class RoomPicsPackageInfoGrantedDepot (
    @ColumnInfo("id") val id: Int,
    @ColumnInfo("depot_id") val depotId: Int,
)