package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.AppId

/**
 * Provides access to user's owned app library.
 */
class Library(
    private val steamClient: SteamClient
): BaseHandler {

    suspend fun requestLibrary() {

    }

    suspend fun requestOwnedApps() {

    }

    suspend fun editCollection() {

    }

    suspend fun createCollection() {

    }

    suspend fun deleteCollection() {

    }

    fun ownsThisApp(appId: AppId) {

    }

    override suspend fun onEvent(packet: SteamPacket) {
        when (packet.messageId) {
            else -> {}
        }
    }
}