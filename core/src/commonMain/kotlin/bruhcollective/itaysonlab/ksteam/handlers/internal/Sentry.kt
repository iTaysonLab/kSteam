package bruhcollective.itaysonlab.ksteam.handlers.internal

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.messages.SteamPacketHeader
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.*
import steam.webui.common.CMsgClientUpdateMachineAuth
import steam.webui.common.CMsgClientUpdateMachineAuthResponse

internal class Sentry(
    private val steamClient: SteamClient
) {
    init {
        steamClient.on(EMsg.k_EMsgClientUpdateMachineAuth) { packet ->
            writeSentryFile(packet.header, CMsgClientUpdateMachineAuth.ADAPTER.decode(packet.payload))
        }
    }

    private fun persistKey(steamId: SteamId) = "sentries.${steamId}"
    private fun sentryFile(steamId: SteamId, fileName: String) = steamClient.workingDirectory / fileName

    private fun sentryFile(steamId: SteamId): Path? {
        return steamClient.persistence.getString(persistKey(steamId))?.let {
            sentryFile(steamId, it)
        }?.takeIf {
            FileSystem.SYSTEM.exists(it)
        }
    }

    fun sentryHash(steamId: SteamId): ByteString? {
        return FileSystem.SYSTEM.read(sentryFile(steamId) ?: return null) {
            readByteString().sha1()
        }
    }

    private suspend fun writeSentryFile(packetHeader: SteamPacketHeader, data: CMsgClientUpdateMachineAuth) =
        withContext(Dispatchers.IO) {
            val currentId = steamClient.currentSessionSteamId
            val filename = data.filename.let { if (it.isNullOrEmpty()) "sentry" else it }
            val filepath = sentryFile(currentId, filename)

            steamClient.persistence.set(persistKey(currentId), filename)

            FileSystem.SYSTEM.write(filepath) {
                write(
                    byteString = data.bytes ?: ByteString.EMPTY,
                    byteCount = data.cubtowrite ?: data.bytes?.size ?: 0,
                    offset = data.offset ?: 0
                )
            }

            steamClient.execute(SteamPacket.newProto(
                EMsg.k_EMsgClientUpdateMachineAuthResponse,
                CMsgClientUpdateMachineAuthResponse(
                    filename = filename,
                    eresult = EResult.OK.encoded,
                    filesize = FileSystem.SYSTEM.openReadOnly(filepath).use {
                        it.size().toInt()
                    },
                    sha_file = sentryHash(currentId),
                    offset = data.offset,
                    cubwrote = data.cubtowrite,
                )
            ).withHeader {
                targetJobId = packetHeader.sourceJobId
            })
        }
}