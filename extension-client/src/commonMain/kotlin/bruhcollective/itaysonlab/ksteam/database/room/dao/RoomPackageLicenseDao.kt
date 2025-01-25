package bruhcollective.itaysonlab.ksteam.database.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import bruhcollective.itaysonlab.ksteam.database.room.entity.apps.RoomPackageLicense

@Dao
internal interface RoomPackageLicenseDao {
    @Query("SELECT * FROM package_license")
    suspend fun getAll(): List<RoomPackageLicense>

    @Query("SELECT * FROM package_license WHERE package_id = :packageId")
    suspend fun getLicensesByPackageId(packageId: Int): List<RoomPackageLicense>

    @Insert
    suspend fun insert(packageLicenses: List<RoomPackageLicense>)

    @Query("SELECT COUNT() FROM package_license")
    suspend fun count(): Int

    @Query("DELETE FROM package_license")
    suspend fun deleteAll()
}