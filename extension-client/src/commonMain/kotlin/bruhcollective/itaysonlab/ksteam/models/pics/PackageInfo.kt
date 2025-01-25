package bruhcollective.itaysonlab.ksteam.models.pics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class PackageInfo {
    @SerialName("packageid")
    var packageId: Int = 0

    @SerialName("billingtype")
    var billingType: Int = 0

    @SerialName("licensetype")
    var licenseType: Int = 0

    @SerialName("status")
    var status: Int = 0

    @SerialName("extended")
    var extended: InfoExtended = InfoExtended()

    @SerialName("appids")
    var appIds: List<Int> = emptyList()

    @SerialName("depotids")
    var depotIds: List<Int> = emptyList()

    @Serializable
    internal class InfoExtended {
        @SerialName("allowcrossregiontradingandgifting")
        var allowInternationalGifting: Boolean = false

        @SerialName("allowpurchasefromrestrictedcountries")
        var allowPurchaseFromRestricted: Boolean = false

        @SerialName("onlyallowrestrictedcountries")
        var onlyAllowRestrictedCountries: Boolean = false

        @SerialName("purchaserestrictedcountries")
        var purchaseRestrictedCountries: String = ""

        @SerialName("restrictedcountries")
        var restrictedCountries: String = ""

        @SerialName("basepackage")
        var basePackageId: Int? = null

        @SerialName("dontgrantifappidowned")
        var dontGrantIfAppOwned: Int? = null

        @SerialName("starttime")
        var startTime: Int = 0

        @SerialName("expirytime")
        var expiryTime: Int = 0

        @SerialName("freepromotion")
        var freePromotion: Boolean = false

        @SerialName("disabletradingcards")
        var disableTradingCards: Boolean = false

        @SerialName("excludefromsharing")
        var excludeFromFamilySharing: Boolean = false
    }
}