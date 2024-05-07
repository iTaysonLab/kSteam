package bruhcollective.itaysonlab.ksteam.handlers.internal

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.models.SteamId

/**
 * Manages file storage for a specific [SteamId].
 */
class Storage internal constructor(
    private val steamClient: SteamClient
) {
    val rootFolder get() = steamClient.config.rootFolder
    fun storageFor(steamId: SteamId) = rootFolder / steamId.id.toString()
}