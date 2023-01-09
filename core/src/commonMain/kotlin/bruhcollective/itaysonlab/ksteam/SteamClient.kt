package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.debug.logDebug
import bruhcollective.itaysonlab.ksteam.handlers.Account
import bruhcollective.itaysonlab.ksteam.handlers.BaseHandler
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.network.CMClient
import bruhcollective.itaysonlab.ksteam.network.CMList
import bruhcollective.itaysonlab.ksteam.web.ExternalWebApi
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Main entrypoint for kSteam usage.
 */
class SteamClient (
    private val config: SteamClientConfiguration = SteamClientConfiguration()
) {
    private val eventsScope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName("kSteam-events"))

    private val externalWebApi = ExternalWebApi(config.networkClient)
    private val serverList = CMList(externalWebApi)
    private val cmClient = CMClient(configuration = config, serverList = serverList)

    val handlers = listOf<BaseHandler>(
        Account(this)
    )

    suspend fun start() {
        cmClient.tryConnect()
    }

    init {
        cmClient.incomingPacketsQueue
            .filter { packet ->
                // We don't need to dispatch targeted packets to the global event queue
                packet.header.targetJobId == 0L
            }.onEach { packet ->
                handlers.forEach { handler ->
                    handler.onEvent(packet)
                }
            }.catch { throwable ->
                logDebug("SteamClient:EventFlow", "Error occurred when collecting a packet: ${throwable.message}")
            }.launchIn(eventsScope)
    }

    inline fun <reified T: BaseHandler> getHandler() = handlers.filterIsInstance<T>().apply {
        require(size == 1) { "Broken handler structure - that's a library issue!" }
    }.first()

    // Low-level packet API goes below. If applicable, use proper bindings.
    suspend fun execute(packet: SteamPacket) = cmClient.execute(packet)
    suspend fun executeAndForget(packet: SteamPacket) = cmClient.executeAndForget(packet)
    val incomingPacketsFlow get() = cmClient.incomingPacketsQueue
}