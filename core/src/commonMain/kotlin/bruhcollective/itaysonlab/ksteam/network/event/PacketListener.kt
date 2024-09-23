package bruhcollective.itaysonlab.ksteam.network.event

import bruhcollective.itaysonlab.ksteam.messages.SteamPacket

fun interface PacketListener {
    fun onPacketReceived(packet: SteamPacket)
}