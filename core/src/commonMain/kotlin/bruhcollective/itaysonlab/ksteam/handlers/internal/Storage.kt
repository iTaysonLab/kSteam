package bruhcollective.itaysonlab.ksteam.handlers.internal

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.handlers.BaseHandler
import bruhcollective.itaysonlab.ksteam.models.SteamId

/**
 * Manages Steam Guard, sessions and caches per-account
 */
class Storage internal constructor(
    private val steamClient: SteamClient
) : BaseHandler {
    val rootFolder get() = steamClient.config.rootFolder

    fun storageFor(steamId: SteamId) = rootFolder / steamId.id.toString()
}