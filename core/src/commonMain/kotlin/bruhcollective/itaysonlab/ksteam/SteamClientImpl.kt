package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.debug.PacketDumper
import bruhcollective.itaysonlab.ksteam.grpc.SteamGrpcClients
import bruhcollective.itaysonlab.ksteam.grpc.SteamGrpcClientsImpl
import bruhcollective.itaysonlab.ksteam.handlers.Account
import bruhcollective.itaysonlab.ksteam.handlers.Configuration
import bruhcollective.itaysonlab.ksteam.handlers.Logger
import bruhcollective.itaysonlab.ksteam.handlers.UnifiedMessages
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.messages.SteamPacketHeader
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.network.CMClient
import bruhcollective.itaysonlab.ksteam.network.CMClientState
import bruhcollective.itaysonlab.ksteam.network.CMList
import bruhcollective.itaysonlab.ksteam.util.CreateSupervisedCoroutineScope
import bruhcollective.itaysonlab.ksteam.web.WebApi
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
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

    private val serverList = CMList(webApi)

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

    override suspend fun start() {
        if (cmNetworkEnabled()) {
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

    override fun on(id: EMsg, consumer: suspend (SteamPacket) -> Unit): Job {
        return cmClient.incomingPacketsQueue.filter { packet ->
            packet.header.targetJobId == 0L && packet.messageId == id
        }.onEach { packet ->
            eventsScope.launch { consumer(packet) }
        }.launchIn(eventsScope)
    }

    override fun onRpc(method: String, consumer: suspend (SteamPacket) -> Unit): Job {
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

    override suspend fun execute(packet: SteamPacket): SteamPacket = requireCmTransport {
        cmClient.execute(packet)
    }

    override suspend fun subscribe(packet: SteamPacket): Flow<SteamPacket> = requireCmTransport {
        cmClient.subscribe(packet)
    }

    override suspend fun executeAndForget(packet: SteamPacket) = requireCmTransport {
        cmClient.executeAndForget(packet)
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