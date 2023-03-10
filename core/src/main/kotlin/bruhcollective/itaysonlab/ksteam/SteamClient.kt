package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.debug.PacketDumper
import bruhcollective.itaysonlab.ksteam.debug.logError
import bruhcollective.itaysonlab.ksteam.extension.Extension
import bruhcollective.itaysonlab.ksteam.extension.HandlerMap
import bruhcollective.itaysonlab.ksteam.handlers.Account
import bruhcollective.itaysonlab.ksteam.handlers.BaseHandler
import bruhcollective.itaysonlab.ksteam.handlers.UnifiedMessages
import bruhcollective.itaysonlab.ksteam.handlers.account
import bruhcollective.itaysonlab.ksteam.handlers.internal.Sentry
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.messages.SteamPacketHeader
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.network.CMClient
import bruhcollective.itaysonlab.ksteam.network.CMClientState
import bruhcollective.itaysonlab.ksteam.network.CMList
import bruhcollective.itaysonlab.ksteam.web.WebApi
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlin.reflect.KClass

class SteamClient internal constructor(
    internal val config: SteamClientConfiguration,
    injectedExtensions: List<Extension>
) {
    private val eventsScope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName("kSteam-events"))

    val webApi = WebApi(config.apiClient)

    private val serverList = CMList(webApi)
    private val cmClient = CMClient(configuration = config, serverList = serverList)

    val handlers: HandlerMap = mapOf<KClass<*>, BaseHandler>(
        Account(this).createAssociation(),
        UnifiedMessages(this).createAssociation(),
        Sentry(this).createAssociation(),
    ) + extensionsToHandlers(injectedExtensions)

    val language get() = config.language

    val connectionStatus get() = cmClient.clientState

    val currentSessionSteamId get() = cmClient.clientSteamId

    /**
     * Manages [PacketDumper] mode.
     *
     * A mode of [PacketDumper.DumpMode.Full] will dump all packets to the "dumps" folder of the kSteam configuration folder.
     */
    var dumperMode: PacketDumper.DumpMode
        get() = cmClient.dumper.dumpMode
        set(value) { cmClient.dumper.dumpMode = value }

    /**
     * Main function, which you need to call before doing anything with kSteam.
     *
     * This will establish connection with Steam Network servers.
     */
    suspend fun start() {
        cmClient.tryConnect()
    }

    init {
        config.apiClient.plugin(HttpSend).intercept { request ->
            if (request.url.pathSegments[request.url.pathSegments.lastIndex - 1] == "GetCMListForConnect") {
                execute(request)
            } else {
                execute(request.writeSteamData()).let { response ->
                    if (response.response.status == HttpStatusCode.Unauthorized) {
                        account.updateAccessToken()
                        execute(request.writeSteamData())
                    } else {
                        response
                    }
                }
            }
        }

        connectionStatus
            .onEach {
                if (it == CMClientState.Logging) {
                    // Now we can log in with a default account if available
                    getHandler<Account>().trySignInSaved()
                }
            }.catch { throwable ->
                logError("SteamClient:EventFlow", "Error occurred when collecting a client state: ${throwable.message}")
            }.launchIn(eventsScope)

        cmClient.incomingPacketsQueue
            .filter { packet ->
                // We don't need to dispatch targeted packets to the global event queue
                packet.header.targetJobId == 0L
            }.onEach { packet ->
                handlers.values.forEach { handler ->
                    try {
                        if (packet.messageId == EMsg.k_EMsgServiceMethod) {
                            handler.onRpcEvent((packet.header as SteamPacketHeader.Protobuf).targetJobName.orEmpty(), packet)
                        } else {
                            handler.onEvent(packet)
                        }
                    } catch (e: Exception) {
                        logError("SteamClient:EventFlow", "Error occurred when collecting a packet: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }.launchIn(eventsScope)
    }

    inline fun <reified T : BaseHandler> getHandler(): T {
        val handler = handlers[T::class]
            ?: throw IllegalStateException("No typed handler registered (trying to get: ${T::class.java.simpleName}).")
        return (handler as? T)
            ?: throw IllegalStateException("Typed handler registered with incorrect mapping (trying to get: ${T::class.java.simpleName}, got: ${handler::class.java.simpleName}).")
    }

    inline fun <reified T> getImplementingHandlerOrNull(): T? {
        return handlers.values.filterIsInstance<T>().firstOrNull()
    }

    private inline fun <reified T : BaseHandler> T.createAssociation() = T::class to this

    private suspend fun HttpRequestBuilder.writeSteamData() = apply {
        account.tokenRequested.first { it }

        if (host == "api.steampowered.com") {
            parameter("access_token", account.getCurrentAccount()?.accessToken.orEmpty())
        } else {
            header("Cookie", "mobileClient=android; mobileClientVersion=777777 3.0.0; steamLoginSecure=${account.buildSteamLoginSecureCookie()};")
        }
    }

    private fun extensionsToHandlers(extensions: List<Extension>): HandlerMap {
        return extensions.map { it.createHandlers(this) }.fold(mutableMapOf()) { map, extensionHandlers ->
            map += extensionHandlers
            map
        }
    }

    suspend fun execute(packet: SteamPacket) = cmClient.execute(packet)
    suspend fun subscribe(packet: SteamPacket) = cmClient.subscribe(packet)
    suspend fun executeAndForget(packet: SteamPacket) = cmClient.executeAndForget(packet)
}