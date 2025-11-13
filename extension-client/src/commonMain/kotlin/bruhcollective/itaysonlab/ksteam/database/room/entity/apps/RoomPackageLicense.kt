package bruhcollective.itaysonlab.ksteam.database.room.entity.apps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import bruhcollective.itaysonlab.ksteam.database.room.TableNames
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplicationLicense
import bruhcollective.itaysonlab.ksteam.models.enums.EPaymentMethod
import steam.messages.clientserver.CMsgClientLicenseList

@Entity(
    tableName = TableNames.PACKAGE_LICENSE,
    indices = [Index(name = "PkgLicenseByPkgId", value = ["package_id"], unique = false)]
)
internal data class RoomPackageLicense(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("id") val id: Long = 0L,

    @ColumnInfo("package_id") val packageId: Int,
    @ColumnInfo("time_created") val timeCreated: Int? = null,
    @ColumnInfo("time_next_process") val timeNextProcess: Int? = null,
    @ColumnInfo("minute_limit") val minuteLimit: Int? = null,
    @ColumnInfo("minutes_used") val minutesUsed: Int? = null,
    @ColumnInfo("payment_method") val paymentMethod: Int? = null,
    @ColumnInfo("flags") val flags: Int? = null,
    @ColumnInfo("purchase_country_code") val purchaseCountryCode: String? = null,
    @ColumnInfo("license_type") val licenseType: Int? = null,
    @ColumnInfo("territory_code") val territoryCode: Int? = null,
    @ColumnInfo("change_number") val changeNumber: Int? = null,
    @ColumnInfo("owner_id") val ownerId: Int? = null,
    @ColumnInfo("initial_period") val initialPeriod: Int? = null,
    @ColumnInfo("initial_time_unit") val initialTimeUnit: Int? = null,
    @ColumnInfo("renewal_period") val renewalPeriod: Int? = null,
    @ColumnInfo("renewal_time_unit") val renewalTimeUnit: Int? = null,
    @ColumnInfo("access_token") val accessToken: Long? = null,
    @ColumnInfo("master_package_id") val masterPackageId: Int? = null,
) {
    constructor(protoLicense: CMsgClientLicenseList.License) : this(
        packageId = protoLicense.package_id ?: 0,
        timeCreated = protoLicense.time_created,
        timeNextProcess = protoLicense.time_next_process,
        minutesUsed = protoLicense.minutes_used,
        paymentMethod = protoLicense.payment_method,
        flags = protoLicense.flags,
        purchaseCountryCode = protoLicense.purchase_country_code,
        licenseType = protoLicense.license_type,
        territoryCode = protoLicense.territory_code,
        changeNumber = protoLicense.change_number,
        ownerId = protoLicense.owner_id,
        initialPeriod = protoLicense.initial_period,
        initialTimeUnit = protoLicense.initial_time_unit,
        renewalPeriod = protoLicense.renewal_time_unit,
        renewalTimeUnit = protoLicense.renewal_time_unit,
        accessToken = protoLicense.access_token,
        masterPackageId = protoLicense.master_package_id,
    )

    fun convert(): SteamApplicationLicense {
        return SteamApplicationLicense(
            packageId = packageId,
            owner = SteamId.fromAccountId(id = ownerId?.toLong() ?: 0),
            purchaseCountryCode = purchaseCountryCode.orEmpty(),
            paymentMethod = EPaymentMethod.byIndex(paymentMethod ?: 0),
            creationTime = timeCreated ?: 0,
            licenseType = licenseType ?: 0,
            demoUsedMinutes = minutesUsed ?: 0,
            demoTotalMinutes = minuteLimit ?: 0,
        )
    }
}