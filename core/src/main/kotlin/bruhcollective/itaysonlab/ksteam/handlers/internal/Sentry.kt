package bruhcollective.itaysonlab.ksteam.handlers.internal

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.handlers.BaseHandler
import bruhcollective.itaysonlab.ksteam.handlers.persona
import bruhcollective.itaysonlab.ksteam.handlers.storage
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.messages.SteamPacketHeader
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.*
import steam.webui.common.CMsgClientUpdateMachineAuth
import steam.webui.common.CMsgClientUpdateMachineAuthResponse
import java.io.File

internal class Sentry(
    private val steamClient: SteamClient
) : BaseHandler {
    private fun sentryFile(steamId: SteamId, fileName: String) = File(steamClient.storage.storageFor(steamId), fileName)

    fun sentryFile(steamId: SteamId): File? {
        return steamClient.storage.globalConfiguration.availableAccounts[steamId.id]?.sentryFileName?.let {
            if (it.isEmpty()) return@let null
            sentryFile(steamId, it)
        }?.takeIf(File::exists)
    }

    fun sentryHash(steamId: SteamId): ByteString? {
        return sentryFile(steamId)?.source()?.buffer()?.use {
            it.readByteString().sha1()
        }
    }

    private suspend fun writeSentryFile(packetHeader: SteamPacketHeader, data: CMsgClientUpdateMachineAuth) =
        withContext(Dispatchers.IO) {
            val currentId = steamClient.persona.currentPersona.value.id
            val filename = data.filename.let { if (it.isNullOrEmpty()) "sentry" else it }

            steamClient.storage.modifyAccount(currentId) {
                copy(sentryFileName = filename)
            }

            sentryFile(currentId, filename).apply {
                if (exists()) delete()
                createNewFile()
            }.sink().buffer().use { sink ->
                sink.write(
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
                    filesize = sentryFile(currentId)?.length()?.toInt(),
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