package bruhcollective.itaysonlab.ksteam.database.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.RoomPicsPackageEntry
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.packages.RoomPicsPackageInfo
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.packages.RoomPicsPackageInfoGrantedApp
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.packages.RoomPicsPackageInfoGrantedDepot
import bruhcollective.itaysonlab.ksteam.models.pics.PackageInfo

@Dao
internal interface RoomPicsPackageDao {
    @Upsert
    suspend fun upsertPackageInfo(
        entries: List<RoomPicsPackageEntry>,
        infos: List<RoomPicsPackageInfo>,
        grantedApps: List<RoomPicsPackageInfoGrantedApp>,
        grantedDepots: List<RoomPicsPackageInfoGrantedDepot>
    )

    @Query("SELECT * FROM package_info WHERE id = :id")
    suspend fun getPackageById(id: Int): RoomPicsPackageInfo?

    @Query("SELECT DISTINCT app_id FROM package_info_apps WHERE id IN (:packageIds)")
    suspend fun getGrantedAppsForPackages(packageIds: List<Int>): List<Int>

    @Query("SELECT DISTINCT id FROM package_info_apps WHERE app_id = :appId")
    suspend fun getGrantedAppConnection(appId: Int): Int?

    @Transaction
    suspend fun upsertPicsPackage(entry: PendingPackageEntry) {
        upsertPackageInfo(
            entries = entry.entries,
            infos = entry.infos,
            grantedApps = entry.grantedApps,
            grantedDepots = entry.grantedDepots
        )
    }

    class PendingPackageEntry {
        val entries: MutableList<RoomPicsPackageEntry> = mutableListOf()
        val infos: MutableList<RoomPicsPackageInfo> = mutableListOf()
        val grantedApps: MutableList<RoomPicsPackageInfoGrantedApp> = mutableListOf()
        val grantedDepots: MutableList<RoomPicsPackageInfoGrantedDepot> = mutableListOf()

        operator fun plusAssign(entry: RoomPicsPackageEntry) {
            entries.add(entry)
        }

        operator fun plusAssign(entry: PackageInfo) {
            infos.add(RoomPicsPackageInfo(entry))

            grantedApps += entry.appIds.map { RoomPicsPackageInfoGrantedApp(id = entry.packageId, appId = it) }
            grantedDepots += entry.depotIds.map { RoomPicsPackageInfoGrantedDepot(id = entry.packageId, depotId = it) }
        }
    }
}