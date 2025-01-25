package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.debug.PacketDumper
import bruhcollective.itaysonlab.ksteam.grpc.SteamGrpcClients
import bruhcollective.itaysonlab.ksteam.grpc.SteamGrpcClientsImpl
import bruhcollective.itaysonlab.ksteam.handlers.Account
import bruhcollective.itaysonlab.ksteam.handlers.Configuration
import bruhcollective.itaysonlab.ksteam.handlers.Logger
import bruhcollective.itaysonlab.ksteam.handlers.UnifiedMessages
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.network.CMClient
import bruhcollective.itaysonlab.ksteam.network.CMClientState
import bruhcollective.itaysonlab.ksteam.network.CMList
import bruhcollective.itaysonlab.ksteam.network.event.PacketListener
import bruhcollective.itaysonlab.ksteam.network.event.TypedProtobufPacketListener
import bruhcollective.itaysonlab.ksteam.util.CreateSupervisedCoroutineScope
import bruhcollective.itaysonlab.ksteam.web.WebApi
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal class SteamClientImpl internal constructor(
    internal val config: SteamClientConfiguration
) : SteamClient {
    private val eventsScope = CreateSupervisedCoroutineScope("events", config.coroutineDispatcher)

    override val configuration: Configuration = Configuration(config.persistenceDriver)
    override val logger: Logger = Logger()
    override val webApi = WebApi(apiClient = config.apiClient, configuration = configuration)
    override val dumper = PacketDumper(saveRootFolder = config.rootFolder, logger = logger)

    private val serverList = CMList(webApi = webApi, logger = logger)

    private val cmClient = CMClient(
        serverList = serverList,
        dumper = dumper,
        logger = logger,
        dispatcher = config.coroutineDispatcher,
        httpClient = config.networkClient
    )

    override val language get() = config.language
    override val workingDirectory get() = config.rootFolder
    override val persistence get() = config.persistenceDriver
    override val deviceInfo get() = config.deviceInfo
    override val authPrivateIpLogic get() = config.authPrivateIpLogic

    override val connectionStatus get() = cmClient.clientState
    override val currentSessionSteamId get() = cmClient.clientSteamId

    // Sub-systems
    override val account: Account = Account(this)
    override val unifiedMessages: UnifiedMessages = UnifiedMessages(this)
    override val grpc: SteamGrpcClients = SteamGrpcClientsImpl(unifiedMessages)

    init {
        config.apiClient.plugin(HttpSend).intercept { request ->
            logger.logVerbose("SteamClientImpl") { "Check for secure HTTP, pass on ${request.url.pathSegments.isEmpty()} or ${request.url.pathSegments[request.url.pathSegments.lastIndex - 1] == "GetCMListForConnect"} (${request.url.pathSegments[request.url.pathSegments.lastIndex]} + ${request.url.pathSegments[request.url.pathSegments.lastIndex - 1]})" }

            if (request.url.pathSegments.isEmpty() || request.url.pathSegments[request.url.pathSegments.lastIndex - 1] == "GetCMListForConnect") {
                logger.logVerbose("SteamClientImpl") { "Passthrough HTTP request" }
                execute(request)
            } else {
                logger.logVerbose("SteamClientImpl") { "Write secure HTTP request" }
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

    override suspend fun start() {
        logger.logVerbose("SteamClientImpl") { "[start]" }

        if (cmNetworkEnabled()) {
            if (serverList.isEmpty()) {
                logger.logVerbose("SteamClientImpl") { "-> refreshServerList" }
                serverList.refreshServerList()
            }

            logger.logVerbose("SteamClientImpl") { "-> tryConnect" }
            cmClient.tryConnect()
        }
    }

    override fun stop() {
        cmClient.stop()
        config.apiClient.close()
        config.networkClient.close()
        eventsScope.cancel()
    }

    override fun onClientState(status: CMClientState, consumer: suspend () -> Unit): Job {
        return connectionStatus.filter { state ->
            state == status
        }.onEach { _ ->
            eventsScope.launch {
                consumer()
            }
        }.launchIn(eventsScope)
    }

    override fun on(id: EMsg, consumer: suspend (SteamPacket) -> Unit): DisposableHandle {
        val listener = PacketListener { packet -> eventsScope.launch { consumer(packet) } }
        cmClient.incomingPacketManager.registerPacketListener(id, listener)
        return DisposableHandle { cmClient.incomingPacketManager.unregisterPacketListener(id, listener) }
    }

    override fun <T: Message<T, *>> onTyped(
        id: EMsg,
        adapter: ProtoAdapter<T>,
        consumer: suspend (T) -> Unit
    ): DisposableHandle {
        val listener = TypedProtobufPacketListener<T> { payload -> eventsScope.launch { consumer(payload) } }
        cmClient.incomingPacketManager.registerTypedPacketListener(id, adapter, listener)
        return DisposableHandle { cmClient.incomingPacketManager.unregisterTypedPacketListener(id, listener) }
    }

    override fun <T: Message<T, *>> onTypedRpc(
        method: String,
        adapter: ProtoAdapter<T>,
        consumer: suspend (T) -> Unit
    ): DisposableHandle {
        val listener = TypedProtobufPacketListener<T> { payload -> eventsScope.launch { consumer(payload) } }
        cmClient.incomingPacketManager.registerTypedRpcListener(method, adapter, listener)
        return DisposableHandle { cmClient.incomingPacketManager.unregisterTypedRpcListener(method, listener) }
    }

    override fun resume() {
        TODO("Not yet implemented")
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override suspend fun execute(packet: SteamPacket) {
        cmClient.execute(packet)
    }

    override suspend fun awaitPacket(packet: SteamPacket): SteamPacket {
        return requireCmTransport { cmClient.executeSingle(packet) }
    }

    override suspend fun <T> awaitProto(
        packet: SteamPacket,
        adapter: ProtoAdapter<T>
    ): T {
        return requireCmTransport { cmClient.executeSingleProtobuf(packet, adapter) }
    }

    override suspend fun <T> awaitMultipleProto(
        packet: SteamPacket,
        adapter: ProtoAdapter<T>,
        stopIf: (T) -> Boolean
    ): List<T> {
        return requireCmTransport { cmClient.executeMultipleProtobuf(packet, adapter, stopIf) }
    }

    override suspend fun <T> awaitStreamedMultipleProto(
        packet: SteamPacket,
        adapter: ProtoAdapter<T>,
        process: suspend (T) -> Boolean
    ) {
        requireCmTransport { cmClient.executeMultipleStreamingProtobuf(packet, adapter, process) }
    }

    override fun cmNetworkEnabled(): Boolean {
        return config.transportMode == SteamClientConfiguration.TransportMode.WebSocket
    }

    private suspend fun HttpRequestBuilder.writeSteamData() = apply {
        account.awaitTokenRequested()

        if (host == "api.steampowered.com") {
            parameter("access_token", account.getCurrentAccount()?.accessToken.orEmpty())
        }

        header("Cookie", account.getWebCookies().joinToString(separator = "; ") { "${it.first}=${it.second}" })
    }

    private suspend inline fun <T> requireCmTransport(crossinline func: suspend () -> T): T {
        if (cmNetworkEnabled()) {
            return func()
        } else {
            throw UnsupportedTransportException()
        }
    }
}