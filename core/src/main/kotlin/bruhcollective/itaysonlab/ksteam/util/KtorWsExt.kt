package bruhcollective.itaysonlab.ksteam.util

import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import io.ktor.websocket.*

internal suspend fun WebSocketSession.send(packet: SteamPacket) = send(Frame.Binary(fin = true, data = packet.encode()))