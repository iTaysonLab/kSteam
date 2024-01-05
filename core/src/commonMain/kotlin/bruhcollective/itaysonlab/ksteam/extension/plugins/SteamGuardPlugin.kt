package bruhcollective.itaysonlab.ksteam.extension.plugins

import bruhcollective.itaysonlab.ksteam.models.SteamId

/**
 * This "plugin" allows independent kSteam extensions to access Steam Guard-related services.
 *
 * Any [Handler] might implement this interface.
 *
 * [Handler]s will be executed in order of addition. If the code is null, kSteam will try to use next [Handler] until a non-null value will be found.
 */
interface SteamGuardPlugin {
    /**
     * Get a static Steam Guard code active at the current time.
     *
     * @return a TOTP code or null if account/guard doesn't exist
     */
    suspend fun getCodeFor(account: SteamId): String?
}