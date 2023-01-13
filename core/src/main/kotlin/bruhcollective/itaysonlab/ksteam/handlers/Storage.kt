package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.storage.GlobalConfiguration
import bruhcollective.itaysonlab.ksteam.models.storage.SavedAccount
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.internal.writeJson
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.json.okio.encodeToBufferedSink
import okio.buffer
import okio.sink
import okio.source
import steam.enums.EMsg
import java.io.File

/**
 * Manages Steam Guard, sessions and caches per-account
 */
internal class Storage(
    private val steamClient: SteamClient
): BaseHandler {
    private val globalConfigFile = File(steamClient.config.rootFolder, "config.json")

    @OptIn(ExperimentalSerializationApi::class)
    internal var globalConfiguration: GlobalConfiguration = Json.decodeFromBufferedSource(globalConfigFile.source().buffer())
        set(value) {
            field = value

            globalConfigFile.let {
                it.delete()
                it.createNewFile()
            }

            Json.encodeToBufferedSink(value, globalConfigFile.sink().buffer())
        }

    fun modifyAccount(steamId: SteamId, func: SavedAccount.() -> SavedAccount) {
        globalConfiguration = globalConfiguration.copy(
            availableAccounts = globalConfiguration.availableAccounts.toMutableMap().apply {
                put(steamId.id, (globalConfiguration.availableAccounts[steamId.id] ?: SavedAccount(steamId = steamId.id)).let(func))
            }
        )
    }

    override suspend fun onEvent(packet: SteamPacket) {
        when (packet.messageId) {
            EMsg.k_EMsgClientLogOnResponse -> {
                // SteamID changed, load the correct storage
            }

            else -> {}
        }
    }
}