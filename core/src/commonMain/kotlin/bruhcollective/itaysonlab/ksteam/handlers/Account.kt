package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket

class Account(
    private val steamClient: SteamClient
): BaseHandler {
    /**
     * Gets all necessary data to generate a sign-in QR code which can be scanned from the mobile app or other kSteam instance.
     *
     * Also starts a polling session.
     */
    suspend fun getSignInQrCode() {

    }

    /**
     * Signs in using a username and a password.
     */
    suspend fun signIn(
        username: String,
        password: String
    ) {

    }

    /**
     * Tries to sign in using saved session data from [Storage].
     * You can specify your own SteamID to sign in a particular account. If this parameter is null, the "default" SteamID will be selected.
     */
    suspend fun trySignInSaved(
        steamId: Long? = null
    ) {

    }

    override suspend fun onEvent(packet: SteamPacket) {
        when (packet.messageId) {

            else -> {}
        }
    }
}