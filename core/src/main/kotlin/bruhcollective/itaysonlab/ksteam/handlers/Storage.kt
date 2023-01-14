package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.storage.GlobalConfiguration
import bruhcollective.itaysonlab.ksteam.models.storage.SavedAccount
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.json.okio.encodeToBufferedSink
import okio.buffer
import okio.sink
import okio.source
import java.io.File

/**
 * Manages Steam Guard, sessions and caches per-account
 */
internal class Storage(
    private val steamClient: SteamClient
): BaseHandler {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val globalConfigFile = File(steamClient.config.rootFolder, "config.json").also {
        if (it.exists().not()) it.createNewFile()
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal var globalConfiguration: GlobalConfiguration = if (globalConfigFile.length() != 0L) {
        globalConfigFile.source().buffer().use {
            json.decodeFromBufferedSource(it)
        }
    } else GlobalConfiguration()
        set(value) {
            field = value
            globalConfigFile.sink().buffer().use {
                json.encodeToBufferedSink(value, it)
            }
        }

    fun modifyAccount(steamId: SteamId, func: SavedAccount.() -> SavedAccount) {
        globalConfiguration = globalConfiguration.copy(
            availableAccounts = globalConfiguration.availableAccounts.toMutableMap().apply {
                put(steamId.id, (globalConfiguration.availableAccounts[steamId.id] ?: SavedAccount(steamId = steamId.id)).let(func))
            }, defaultAccount = if (globalConfiguration.defaultAccount == 0u.toULong()) {
                steamId.id
            } else {
                globalConfiguration.defaultAccount
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