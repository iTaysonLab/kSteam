package bruhcollective.itaysonlab.ksteam.models.app

import bruhcollective.itaysonlab.ksteam.SteamClient

/**
 * Describes an owned Steam application.
 *
 * This is [SteamApplication], but augmented with license data.
 */
data class OwnedSteamApplication (
    /**
     * The actual [SteamApplication] from shared PICS data.
     */
    val application: SteamApplication,

    /**
     * Copies that are available for an account.
     */
    val licenses: List<SteamApplicationLicense> = emptyList(),

    /**
     * Playtime information for the current account.
     *
     * Will be empty if this object was not part of library query result.
     */
    val playTime: SteamApplicationPlaytime? = null
) {
    /**
     * Returns if the current signed-in user owns this application.
     */
    fun ownsThisApp(steamClient: SteamClient): Boolean {
        return licenses.any { it.owner == steamClient.currentSessionSteamId }
    }
}