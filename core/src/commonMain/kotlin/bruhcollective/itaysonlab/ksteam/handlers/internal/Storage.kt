package bruhcollective.itaysonlab.ksteam.handlers.internal

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.handlers.BaseHandler
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.storage.GlobalConfiguration
import bruhcollective.itaysonlab.ksteam.models.storage.SavedAccount
import bruhcollective.itaysonlab.ksteam.platform.fileProxiedObject
import bruhcollective.itaysonlab.ksteam.platform.getValue
import bruhcollective.itaysonlab.ksteam.platform.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * Manages Steam Guard, sessions and caches per-account
 */
class Storage internal constructor(
    private val steamClient: SteamClient
) : BaseHandler {
    val rootFolder get() = steamClient.config.rootFolder

    internal var globalConfiguration by fileProxiedObject(rootFolder / "config.json", GlobalConfiguration())

    internal suspend fun modifyAccount(steamId: SteamId, func: SavedAccount.() -> SavedAccount) = withContext(Dispatchers.IO) {
        globalConfiguration = globalConfiguration.copy(
            availableAccounts = globalConfiguration.availableAccounts.toMutableMap().apply {
                put(
                    steamId.id,
                    (globalConfiguration.availableAccounts[steamId.id] ?: SavedAccount(steamId = steamId.id)).let(func)
                )
            }, defaultAccount = if (globalConfiguration.defaultAccount == 0u.toULong()) {
                steamId.id
            } else {
                globalConfiguration.defaultAccount
            }
        )
    }

    fun storageFor(steamId: SteamId) = steamClient.config.rootFolder / steamId.id.toString()
}