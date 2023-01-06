package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.messages.BasePacketMessage
import bruhcollective.itaysonlab.ksteam.network.CMList
import bruhcollective.itaysonlab.ksteam.network.WebSocketCMClient
import bruhcollective.itaysonlab.ksteam.web.WebApi

/**
 * Main entrypoint for kSteam usage.
 */
class SteamClient (
    private val config: SteamClientConfiguration = SteamClientConfiguration()
) {
    private val webApi = WebApi(config.networkClient)
    private val serverList = CMList(webApi)
    private val cmClient = WebSocketCMClient(configuration = config, serverList = serverList)

    suspend fun start() {
        cmClient.tryConnect()
    }

    suspend fun <Request, Response> execute(packet: BasePacketMessage<Request>) = cmClient.execute<Request, Response>(packet)
}