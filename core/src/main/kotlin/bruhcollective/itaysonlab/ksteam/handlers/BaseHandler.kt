package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.messages.SteamPacket

interface BaseHandler {
    suspend fun onEvent(packet: SteamPacket)
}