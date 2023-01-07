package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.network.CMList
import bruhcollective.itaysonlab.ksteam.network.CMClient
import bruhcollective.itaysonlab.ksteam.web.WebApi

/**
 * Main entrypoint for kSteam usage.
 */
class SteamClient (
    private val config: SteamClientConfiguration = SteamClientConfiguration()
) {
    private val webApi = WebApi(config.networkClient)
    private val serverList = CMList(webApi)
    private val cmClient = CMClient(configuration = config, serverList = serverList)

    suspend fun start() {
        cmClient.tryConnect()
    }

    // Low-level packet API goes below. If applicable, use proper bindings.
    suspend fun execute(packet: SteamPacket) = cmClient.execute(packet)
    suspend fun executeAndForget(packet: SteamPacket) = cmClient.executeAndForget(packet)
    val incomingPacketsFlow get() = cmClient.incomingPacketsQueue
}