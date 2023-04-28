package bruhcollective.itaysonlab.ksteam.handlers.internal

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.handlers.BaseHandler
import bruhcollective.itaysonlab.ksteam.handlers.storage
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.messages.SteamPacketHeader
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.platform.provideOkioFilesystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.ByteString
import okio.Path
import okio.use
import steam.webui.common.CMsgClientUpdateMachineAuth
import steam.webui.common.CMsgClientUpdateMachineAuthResponse

internal class Sentry(
    private val steamClient: SteamClient
) : BaseHandler {
    private fun sentryFile(steamId: SteamId, fileName: String) = steamClient.storage.storageFor(steamId) / fileName

    private fun sentryFile(steamId: SteamId): Path? {
        return steamClient.storage.globalConfiguration.availableAccounts[steamId.id]?.sentryFileName?.let {
            if (it.isEmpty()) return@let null
            sentryFile(steamId, it)
        }?.takeIf {
            provideOkioFilesystem().exists(it)
        }
    }

    fun sentryHash(steamId: SteamId): ByteString? {
        return provideOkioFilesystem().read(sentryFile(steamId) ?: return null) {
            readByteString().sha1()
        }
    }

    private suspend fun writeSentryFile(packetHeader: SteamPacketHeader, data: CMsgClientUpdateMachineAuth) =
        withContext(Dispatchers.IO) {
            val fs = provideOkioFilesystem()

            val currentId = steamClient.currentSessionSteamId
            val filename = data.filename.let { if (it.isNullOrEmpty()) "sentry" else it }
            val filepath = sentryFile(currentId, filename)

            steamClient.storage.modifyAccount(currentId) {
                copy(sentryFileName = filename)
            }

            fs.write(filepath) {
                write(
                    byteString = data.bytes ?: ByteString.EMPTY,
                    byteCount = data.cubtowrite ?: data.bytes?.size ?: 0,
                    offset = data.offset ?: 0
                )
            }

            steamClient.executeAndForget(SteamPacket.newProto(
                EMsg.k_EMsgClientUpdateMachineAuthResponse,
                CMsgClientUpdateMachineAuthResponse.ADAPTER,
                CMsgClientUpdateMachineAuthResponse(
                    filename = filename,
                    eresult = EResult.OK.encoded,
                    filesize = fs.openReadOnly(filepath).use {
                        it.size().toInt()
                    },
                    sha_file = sentryHash(currentId),
                    offset = data.offset,
                    cubwrote = data.cubtowrite,
                )
            ).apply {
                header.targetJobId = packetHeader.sourceJobId
            })
        }

    override suspend fun onEvent(packet: SteamPacket) {
        if (packet.messageId == EMsg.k_EMsgClientUpdateMachineAuth) {
            writeSentryFile(packet.header, packet.getProtoPayload(CMsgClientUpdateMachineAuth.ADAPTER).data)
        }
    }
}