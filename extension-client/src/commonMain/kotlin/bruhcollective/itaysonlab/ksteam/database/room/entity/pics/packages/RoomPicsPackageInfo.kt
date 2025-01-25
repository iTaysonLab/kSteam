package bruhcollective.itaysonlab.ksteam.database.room.entity.pics.packages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import bruhcollective.itaysonlab.ksteam.database.room.TableNames
import bruhcollective.itaysonlab.ksteam.database.room.entity.pics.RoomPicsPackageEntry
import bruhcollective.itaysonlab.ksteam.models.pics.PackageInfo

// appIds/depotIds are in separate tables
@Entity(tableName = TableNames.PACKAGE_INFO, foreignKeys = [
    ForeignKey(
        entity = RoomPicsPackageEntry::class,
        parentColumns = ["id"],
        childColumns = ["id"],
        onDelete = ForeignKey.CASCADE
    )
])
internal data class RoomPicsPackageInfo (
    @PrimaryKey @ColumnInfo("id") val id: Int,

    // VDF information
    @ColumnInfo("billingtype") val billingType: Int,
    @ColumnInfo("licensetype") val licenseType: Int,
    @ColumnInfo("status") val status: Int,
    @ColumnInfo("allowcrossregiontradingandgifting") val allowInternationalGifting: Boolean,
    @ColumnInfo("allowpurchasefromrestrictedcountries") val allowPurchaseFromRestricted: Boolean,
    @ColumnInfo("onlyallowrestrictedcountries") val onlyAllowRestrictedCountries: Boolean,
    @ColumnInfo("purchaserestrictedcountries") val purchaseRestrictedCountries: String,
    @ColumnInfo("restrictedcountries") val restrictedCountries: String,
    @ColumnInfo("basepackage") val basePackageId: Int?,
    @ColumnInfo("dontgrantifappidowned") val dontGrantIfAppOwned: Int?,
    @ColumnInfo("starttime") val startTime: Int,
    @ColumnInfo("expirytime") val expiryTime: Int,
    @ColumnInfo("freepromotion") val freePromotion: Boolean,
    @ColumnInfo("disabletradingcards") val disableTradingCards: Boolean,
    @ColumnInfo("excludefromsharing") val excludeFromFamilySharing: Boolean
) {
    constructor(ks: PackageInfo): this(
        id = ks.packageId,
        billingType = ks.billingType,
        licenseType = ks.licenseType,
        status = ks.status,
        allowInternationalGifting = ks.extended.allowInternationalGifting,
        allowPurchaseFromRestricted = ks.extended.allowPurchaseFromRestricted,
        onlyAllowRestrictedCountries = ks.extended.onlyAllowRestrictedCountries,
        purchaseRestrictedCountries = ks.extended.purchaseRestrictedCountries,
        restrictedCountries = ks.extended.restrictedCountries,
        basePackageId = ks.extended.basePackageId,
        dontGrantIfAppOwned = ks.extended.dontGrantIfAppOwned,
        startTime = ks.extended.startTime,
        expiryTime = ks.extended.expiryTime,
        freePromotion = ks.extended.freePromotion,
        disableTradingCards = ks.extended.disableTradingCards,
        excludeFromFamilySharing = ks.extended.excludeFromFamilySharing,
    )
}
