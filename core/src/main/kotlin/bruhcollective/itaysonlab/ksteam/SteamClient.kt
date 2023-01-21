package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.debug.logError
import bruhcollective.itaysonlab.ksteam.handlers.*
import bruhcollective.itaysonlab.ksteam.handlers.Storage
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.network.CMClient
import bruhcollective.itaysonlab.ksteam.network.CMClientState
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
import kotlin.reflect.KClass

/**
 * Main entrypoint for kSteam usage.
 */
class SteamClient (
    internal val config: SteamClientConfiguration
) {
    private val eventsScope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName("kSteam-events"))

    internal val externalWebApi = ExternalWebApi(config.networkClient)
    private val serverList = CMList(externalWebApi)
    private val cmClient = CMClient(configuration = config, serverList = serverList)

    val handlers = mapOf<KClass<*>, BaseHandler>(
        Account(this).createAssociation(),
        WebApi(this).createAssociation(),
        Storage(this).createAssociation(),
        Persona(this).createAssociation(),
        Notifications(this).createAssociation(),
        Store(this).createAssociation(),
        Library(this).createAssociation(),
        Sentry(this).createAssociation(),
        Guard(this).createAssociation(),
    )

    val connectionStatus get() = cmClient.clientState
    val incomingPacketsFlow get() = cmClient.incomingPacketsQueue

    suspend fun start() {
        cmClient.tryConnect()
    }

    init {
        connectionStatus
            .onEach {
                if (it == CMClientState.Logging) {
                    // Now we can log in with a default account if available
                    getHandler<Account>().trySignInSaved()
                }
            }.catch { throwable ->
                logError("SteamClient:EventFlow", "Error occurred when collecting a client state: ${throwable.message}")
            }.launchIn(eventsScope)

        incomingPacketsFlow
            .filter { packet ->
                // We don't need to dispatch targeted packets to the global event queue
                packet.header.targetJobId == 0L
            }.onEach { packet ->
                handlers.values.forEach { handler ->
                    handler.onEvent(packet)
                }
            }.catch { throwable ->
                logError("SteamClient:EventFlow", "Error occurred when collecting a packet: ${throwable.message}")
            }.launchIn(eventsScope)
    }

    inline fun <reified T: BaseHandler> getHandler(): T {
        val handler = handlers[T::class] ?: throw IllegalStateException("No typed handler registered (trying to get: ${T::class.java.simpleName}).")
        return (handler as? T) ?: throw IllegalStateException("Typed handler registered with incorrect mapping (trying to get: ${T::class.java.simpleName}, got: ${handler::class.java.simpleName}).")
    }

    private inline fun <reified T: BaseHandler> T.createAssociation() = T::class to this

    suspend fun execute(packet: SteamPacket) = cmClient.execute(packet)
    suspend fun executeAndForget(packet: SteamPacket) = cmClient.executeAndForget(packet)
}