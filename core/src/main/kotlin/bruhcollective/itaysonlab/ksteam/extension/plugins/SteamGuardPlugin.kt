package bruhcollective.itaysonlab.ksteam.extension.plugins

import bruhcollective.itaysonlab.ksteam.models.SteamId

/**
 * This "plugin" allows independent kSteam extensions to access Steam Guard-related services.
 *
 * Any [Handler] might implement this interface, but the first added in order will be used.
 */
interface SteamGuardPlugin {
    /**
     * Get a static Steam Guard code active at the current time.
     *
     * @return a TOTP code or null if account/guard doesn't exist
     */
    suspend fun getCodeFor(account: SteamId): String?
}