package bruhcollective.itaysonlab.ksteam.models.pics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PackageInfo internal constructor(
    @SerialName("packageid") val packageId: Int,
    @SerialName("billingtype") val billingType: Int,
    @SerialName("licensetype") val licenseType: Int,
    val status: Int,
    val extended: PackageInfoExtended = PackageInfoExtended(),
    @SerialName("appids") val appIds: List<Int> = emptyList(),
    @SerialName("depotids") val depotIds: List<Int> = emptyList()
) {
    @Serializable
    data class PackageInfoExtended internal constructor(
        @SerialName("allowcrossregiontradingandgifting") val allowInternationalGifting: Boolean = false,
        @SerialName("allowpurchasefromrestrictedcountries") val allowPurchaseFromRestricted: Boolean = false,
        @SerialName("onlyallowrestrictedcountries") val onlyAllowRestrictedCountries: Boolean = false,
        @SerialName("purchaserestrictedcountries") val purchaseRestrictedCountries: String = "",
        @SerialName("restrictedcountries") val restrictedCountries: String = "",
        @SerialName("basepackage") val basePackageId: Int? = null,
        @SerialName("dontgrantifappidowned") val dontGrantIfAppOwned: Int? = null,
        @SerialName("starttime") val startTime: Int = 0,
        @SerialName("expirytime") val expiryTime: Int = 0,
        @SerialName("freepromotion") val freePromotion: Boolean = false,
        @SerialName("disabletradingcards") val disableTradingCards: Boolean = false,
        @SerialName("excludefromsharing") val excludeFromFamilySharing: Boolean = false,
    )
}