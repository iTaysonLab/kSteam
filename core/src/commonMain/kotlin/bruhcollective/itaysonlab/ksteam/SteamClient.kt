package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.debug.PacketDumper
import bruhcollective.itaysonlab.ksteam.grpc.SteamGrpcClients
import bruhcollective.itaysonlab.ksteam.grpc.SteamGrpcClientsImpl
import bruhcollective.itaysonlab.ksteam.handlers.Account
import bruhcollective.itaysonlab.ksteam.handlers.Configuration
import bruhcollective.itaysonlab.ksteam.handlers.Logger
import bruhcollective.itaysonlab.ksteam.handlers.UnifiedMessages
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * The actual kSteam main client which processes requests to/from Steam.
 *
 * For creating a kSteam instance, use the [kSteam] function instead, which provides more user-friendly configuration.
 */
class SteamClient internal constructor(
    internal val config: SteamClientConfiguration
) {
    private val eventsScope = CreateSupervisedCoroutineScope("events", config.coroutineDispatcher)

    val configuration: Configuration = Configuration(this)
    val logger: Logger = Logger()
    val webApi = WebApi(apiClient = config.apiClient, configuration = configuration)
    val dumper = PacketDumper(saveRootFolder = config.rootFolder, logger = logger)

    private val serverList = CMList(webApi)

    private val cmClient = CMClient(
        serverList = serverList,
        dumper = dumper,
        logger = logger,
        dispatcher = config.coroutineDispatcher,
        httpClient = config.networkClient
    )

    val language get() = config.language
    val workingDirectory get() = config.rootFolder
    val persistence get() = config.persistenceDriver
    val connectionStatus get() = cmClient.clientState
    val currentSessionSteamId get() = cmClient.clientSteamId

    // Sub-systems
    val account: Account = Account(this)
    val unifiedMessages: UnifiedMessages = UnifiedMessages(this)
    val storage: Storage = Storage(this)
    val grpc: SteamGrpcClients = SteamGrpcClientsImpl(unifiedMessages)
    internal val sentry: Sentry = Sentry(this)

    /**
     * Main function, which you need to call before doing anything with kSteam.
     *
     * This will establish connection with Steam Network servers.
     */
    suspend fun start() {
        if (cmNetworkEnabled()) {
            cmClient.tryConnect()
        }
    }

    /**
     * Stops this client. Also releases resources used by networking layer.
     *
     * You should not use this instance of [SteamClient] after calling [stop].
     */
    fun stop() {
        cmClient.stop()
        config.apiClient.close()
        config.networkClient.close()
        eventsScope.cancel()
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
    }

    /**
     * Subscribes to a change of [CMClientState].
     *
     * @param status what status is required to have
     * @return a [Job] to cancel the execution if needed
     */
    fun onClientState(status: CMClientState, consumer: suspend () -> Unit): Job {
        return connectionStatus.filter { state ->
            state == status
        }.onEach { _ ->
            eventsScope.launch {
                consumer()
            }
        }.launchIn(eventsScope)
    }

    /**
     * Subscribes to incoming messages of the specific type. This will only process "jobless" messages (messages that are not a response to something).
     *
     * @param id a [EMsg] that needs to be received
     * @param consumer a receiver for incoming messages
     * @return a [Job] to cancel the execution if needed
     */
    fun on(id: EMsg, consumer: suspend (SteamPacket) -> Unit): Job {
        return cmClient.incomingPacketsQueue.filter { packet ->
            packet.header.targetJobId == 0L && packet.messageId == id
        }.onEach { packet ->
            eventsScope.launch { consumer(packet) }
        }.launchIn(eventsScope)
    }

    /**
     * Subscribes to incoming RPC messages of the specific type. This will only process "jobless" messages (messages that are not a response to something).
     *
     * @param method a RPC definition like "Service.Message"
     * @param consumer a receiver for incoming messages
     * @return a [Job] to cancel the execution if needed
     */
    fun onRpc(method: String, consumer: suspend (SteamPacket) -> Unit): Job {
        return cmClient.incomingPacketsQueue.filter { packet ->
            packet.header.targetJobId == 0L && packet.messageId == EMsg.k_EMsgServiceMethod
        }.onEach { packet ->
            if ((packet.header as SteamPacketHeader.Protobuf).targetJobName == method) {
                eventsScope.launch {
                    consumer(packet)
                }
            }
        }.launchIn(eventsScope)
    }

    /**
     * Execute a [SteamPacket] and await for a response.
     */
    suspend fun execute(packet: SteamPacket): SteamPacket = requireCmTransport {
        cmClient.execute(packet)
    }

    /**
     * Execute a [SteamPacket] and subscribe for a set of responses. It is the caller responsibility to close the [Flow].
     */
    suspend fun subscribe(packet: SteamPacket): Flow<SteamPacket> = requireCmTransport {
        cmClient.subscribe(packet)
    }

    /**
     * Execute a [SteamPacket] without waiting for a response.
     */
    suspend fun executeAndForget(packet: SteamPacket) = requireCmTransport {
        cmClient.executeAndForget(packet)
    }

    private suspend fun HttpRequestBuilder.writeSteamData() = apply {
        account.tokenRequested.first { it }

        if (host == "api.steampowered.com") {
            parameter("access_token", account.getCurrentAccount()?.accessToken.orEmpty())
        }

        header("Cookie", "mobileClient=android; mobileClientVersion=777777 3.7.4; steamLoginSecure=${account.buildSteamLoginSecureCookie()};")
    }

    internal fun cmNetworkEnabled() = config.transportMode == SteamClientConfiguration.TransportMode.WebSocket

    private suspend inline fun <T> requireCmTransport(crossinline func: suspend () -> T): T {
        if (cmNetworkEnabled()) {
            return func()
        } else {
            throw UnsupportedTransportException()
        }
    }
}