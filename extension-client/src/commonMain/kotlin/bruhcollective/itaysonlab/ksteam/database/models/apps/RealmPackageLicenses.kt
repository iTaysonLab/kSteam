package bruhcollective.itaysonlab.ksteam.database.models.apps

import bruhcollective.itaysonlab.ksteam.database.models.ConvertsTo
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.app.SteamApplicationLicense
import bruhcollective.itaysonlab.ksteam.models.enums.EPaymentMethod
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import steam.webui.common.CMsgClientLicenseList_License

/**
 * Defines account-specific license information got from ClientLicenseList.
 *
 * This allows easier querying for family shared copies, for example.
 */
class RealmPackageLicenses(): RealmObject, ConvertsTo<List<SteamApplicationLicense>> {
    @PrimaryKey
    var packageId: Int = 0

    var licenses: RealmList<RealmPackageLicense> = realmListOf()

    class RealmPackageLicense(): EmbeddedRealmObject {
        var ownerId: Int = 0
        var purchaseCountryCode: String = ""
        var paymentMethod: Int = 0
        var creationTime: Int = 0
        var licenseType: Int = 0
        var minuteLimit: Int = 0
        var minutesUsed: Int = 0

        constructor(protoLicense: CMsgClientLicenseList_License): this() {
            ownerId = protoLicense.owner_id ?: 0
            paymentMethod = protoLicense.payment_method ?: 0
            creationTime = protoLicense.time_created ?: 0
            licenseType = protoLicense.license_type ?: 0
            minuteLimit = protoLicense.minute_limit ?: 0
            minutesUsed = protoLicense.minutes_used ?: 0
            purchaseCountryCode = protoLicense.purchase_country_code ?: ""
        }
    }

    override fun convert(): List<SteamApplicationLicense> {
        return licenses.map { license ->
            SteamApplicationLicense(
                owner = SteamId.fromAccountId(id = license.ownerId.toLong()),
                purchaseCountryCode = license.purchaseCountryCode,
                paymentMethod = EPaymentMethod.byIndex(license.paymentMethod),
                creationTime = license.creationTime,
                licenseType = license.licenseType,
                packageId = packageId,
                demoUsedMinutes = license.minutesUsed,
                demoTotalMinutes = license.minuteLimit,
            )
        }
    }
}