package bruhcollective.itaysonlab.ksteam.models.app

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EPaymentMethod

/**
 * Describes an owned Steam application license.
 */
data class SteamApplicationLicense (
    /**
     * License owner SteamID.
     */
    val owner: SteamId,

    /**
     * Package ID that is granted by this license. Packages include app and depot IDs.
     */
    val packageId: Int,

    /**
     * Creation time - mostly the purchase date (?)
     */
    val creationTime: Int,

    /**
     * For demos: amount of minutes that have been used.
     */
    val demoUsedMinutes: Int,

    /**
     * For demos: amount of minutes that are allowed to use (2 hours demo -> 120 minutes).
     */
    val demoTotalMinutes: Int,

    /**
     * Payment method that was used to acquire this license.
     */
    val paymentMethod: EPaymentMethod,

    /**
     * Country code of the account on the moment of acquiring this license.
     */
    val purchaseCountryCode: String,

    /**
     * License type.
     */
    val licenseType: Int
)