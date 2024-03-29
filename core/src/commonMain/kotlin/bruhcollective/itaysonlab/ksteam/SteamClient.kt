package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.debug.PacketDumper
import bruhcollective.itaysonlab.ksteam.extension.Extension
import bruhcollective.itaysonlab.ksteam.extension.HandlerMap
import bruhcollective.itaysonlab.ksteam.handlers.Account
import bruhcollective.itaysonlab.ksteam.handlers.BaseHandler
import bruhcollective.itaysonlab.ksteam.handlers.Configuration
import bruhcollective.itaysonlab.ksteam.handlers.UnifiedMessages
import bruhcollective.itaysonlab.ksteam.handlers.account
import bruhcollective.itaysonlab.ksteam.handlers.internal.Sentry
import bruhcollective.itaysonlab.ksteam.handlers.internal.Storage
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.messages.SteamPacketHeader
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.network.CMClient
import bruhcollective.itaysonlab.ksteam.network.CMClientState
import bruhcollective.itaysonlab.ksteam.network.CMList
import bruhcollective.itaysonlab.ksteam.util.CreateSupervisedCoroutineScope
import bruhcollective.itaysonlab.ksteam.web.WebApi
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * The actual kSteam main client which processes requests to/from Steam.
 *
 * For creating a kSteam instance, use the [kSteam] function instead, which provides more user-friendly configuration.
 */
class SteamClient internal constructor(
    internal val config: SteamClientConfiguration,
    injectedExtensions: List<Extension>
) {
    private val eventsScope = CreateSupervisedCoroutineScope("kSteam-events", Dispatchers.IO)

    val webApi = WebApi(config.apiClient)

    private val serverList = CMList(webApi)
    private val cmClient = CMClient(configuration = config, serverList = serverList)

    val handlers: HandlerMap = mutableMapOf<KClass<*>, BaseHandler>(
        Configuration(this).createAssociation(),
        Account(this).createAssociation(),
        UnifiedMessages(this).createAssociation(),
        Sentry(this).createAssociation(),
        Storage(this).createAssociation(),
    ) bindExtensions injectedExtensions

    val language get() = config.language
    val workingDirectory get() = config.rootFolder
    val persistence get() = config.persistenceDriver

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

    /**
     * Stops this client. Also releases resources used by networking layer.
     *
     * You should not use this instance of [SteamClient] after calling [stop].
     */
    fun stop() {
        cmClient.stop()
        eventsScope.cancel()
        handlers.values.forEach(BaseHandler::onClose)
    }

    init {
        config.apiClient.plugin(HttpSend).intercept { request ->
            if (request.url.pathSegments.isEmpty() || request.url.pathSegments[request.url.pathSegments.lastIndex - 1] == "GetCMListForConnect") {
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

        eventsScope.launch {
            connectionStatus.filter { state ->
                state == CMClientState.AwaitingAuthorization
            }.collect { _ ->
                runCatching {
                    getHandler<Account>().trySignInSaved()
                }.onFailure { e ->
                    KSteamLogging.logError("SteamClient:EventFlow") { "Error occurred when collecting a client state: ${e.message}" }
                }
            }
        }

        eventsScope.launch {
            cmClient.incomingPacketsQueue.filter { packet ->
                // We don't need to dispatch targeted packets to the global event queue
                packet.header.targetJobId == 0L
            }.collect { packet ->
                for (handler in handlers.values) {
                    kotlin.runCatching {
                        if (packet.messageId == EMsg.k_EMsgServiceMethod) {
                            handler.onRpcEvent(
                                (packet.header as SteamPacketHeader.Protobuf).targetJobName.orEmpty(),
                                packet
                            )
                        } else {
                            handler.onEvent(packet)
                        }
                    }.onFailure { e ->
                        KSteamLogging.logError("SteamClient:EventFlow") { "Error occurred when collecting a packet: ${e.message}" }
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    /**
     * Return a [BaseHandler] registered on the [SteamClient] initialization.
     *
     * A [BaseHandler] is an abstract which provides separated Steam controls.
     */
    inline fun <reified T : BaseHandler> getHandler(): T {
        val handler = handlers[T::class]
            ?: throw IllegalStateException("No typed handler registered (trying to get: ${T::class.simpleName}).")
        return (handler as? T)
            ?: throw IllegalStateException("Typed handler registered with incorrect mapping (trying to get: ${T::class.simpleName}, got: ${handler::class.simpleName}).")
    }

    /**
     * A [getHandler] alternative to find a plugin.
     *
     * A plugin in kSteam is an "abstract" plug-in [BaseHandler] that can be used in the core module/extensions without the need to depend on a specific implementation.
     * For example, a "core" module might use a metadata plugin to prefer cached for saving some bandwidth.
     */
    inline fun <reified T> getImplementingHandlerOrNull(): T? {
        return handlers.values.filterIsInstance<T>().firstOrNull()
    }

    /**
     * A [getHandler] alternative to find a set of plugins.
     *
     * A plugin in kSteam is an "abstract" plug-in [BaseHandler] that can be used in the core module/extensions without the need to depend on a specific implementation.
     * For example, a "core" module might use a metadata plugin to prefer cached for saving some bandwidth.
     */
    inline fun <reified T> getImplementingHandlers(): List<T> {
        return handlers.values.filterIsInstance<T>()
    }

    private inline fun <reified T : BaseHandler> T.createAssociation() = T::class to this

    private suspend fun HttpRequestBuilder.writeSteamData() = apply {
        account.tokenRequested.first { it }

        if (host == "api.steampowered.com") {
            parameter("access_token", account.getCurrentAccount()?.accessToken.orEmpty())
        }

        header("Cookie", "mobileClient=android; mobileClientVersion=777777 3.7.4; steamLoginSecure=${account.buildSteamLoginSecureCookie()};")
    }

    private infix fun MutableMap<KClass<*>, BaseHandler>.bindExtensions(extensions: List<Extension>) = apply {
        extensions.forEach {
            putAll(it.createHandlers(this@SteamClient))
        }
    }

    /**
     * Execute a [SteamPacket] and await for a response.
     *
     * Most developers won't need to use this method directly if there is a matching [BaseHandler] for the task.
     */
    suspend fun execute(packet: SteamPacket) = cmClient.execute(packet)

    /**
     * Execute a [SteamPacket] and subscribe for a set of responses. It is the caller responsibility to close the [Flow].
     *
     * Most developers won't need to use this method directly if there is a matching [BaseHandler] for the task.
     */
    suspend fun subscribe(packet: SteamPacket) = cmClient.subscribe(packet)

    /**
     * Execute a [SteamPacket] without waiting for a response.
     *
     * Most developers won't need to use this method directly if there is a matching [BaseHandler] for the task.
     */
    suspend fun executeAndForget(packet: SteamPacket) = cmClient.executeAndForget(packet)
}