@file:UseSerializers(
    MutableRealmIntKSerializer::class,
    RealmAnyKSerializer::class,
    RealmDictionaryKSerializer::class,
    RealmInstantKSerializer::class,
    RealmListKSerializer::class,
    RealmSetKSerializer::class,
    RealmUUIDKSerializer::class
)

package bruhcollective.itaysonlab.ksteam.models.pics

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.serializers.*
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
internal class PackageInfo: RealmObject {
    @PrimaryKey
    @SerialName("packageid")
    var packageId: Int = 0

    @SerialName("billingtype")
    var billingType: Int = 0

    @SerialName("licensetype")
    var licenseType: Int = 0

    @SerialName("status")
    var status: Int = 0

    @SerialName("extended")
    var extended: InfoExtended? = null

    @SerialName("appids")
    var appIds: RealmList<Int> = realmListOf()

    @SerialName("depotids")
    var depotIds: RealmList<Int> = realmListOf()

    @Serializable
    internal class InfoExtended: EmbeddedRealmObject {
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