package bruhcollective.itaysonlab.ksteam.network

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.debug.PacketDumper
import bruhcollective.itaysonlab.ksteam.handlers.Logger
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.util.CreateSupervisedCoroutineScope
import bruhcollective.itaysonlab.ksteam.web.models.CMServerEntry
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import okio.Buffer
import okio.Source
import okio.buffer
import okio.gzip
import steam.messages.clientserver_login.CMsgClientHello
import steam.webui.common.CMsgClientHeartBeat
import steam.webui.common.CMsgClientLogonResponse
import steam.webui.common.CMsgMulti

/**
 * This class manages connection to the CM network on Steam.
 */
internal class CMClient(
    private val serverList: CMList,
    private val dumper: PacketDumper,
    private val logger: Logger,
    private val httpClient: HttpClient,
    dispatcher: CoroutineDispatcher
) {
    // A scope used to hold WSS connection
    private val internalScope = CreateSupervisedCoroutineScope("cmClient", dispatcher) { _, throwable ->
        throwable.printStackTrace()
        mutableClientState.value = CMClientState.Offline
        launchConnectionCoroutine(reconnect = true)
    }

    private var selectedServer: CMServerEntry? = null

    /**
     * A counter used to set sourceJobID field in the Steam packets.
     */
    private var jobIdCounter = 0L

    private var cellId = 0
    internal var clientSessionId = 0
    internal var clientSteamId = SteamId.Empty

    /**
     * A queue for outgoing packets. These will be collected in the WebSocket loop and sent to the server.
     */
    private val outgoingPacketsQueue = Channel<SteamPacket>(capacity = Channel.UNLIMITED)

    /**
     * A queue for incoming packets, which are processed by consumers
     */
    private val mutableIncomingPacketsQueue = MutableSharedFlow<SteamPacket>(extraBufferCapacity = Channel.UNLIMITED)
    val incomingPacketsQueue = mutableIncomingPacketsQueue.asSharedFlow()

    /**
     * CMClient state
     */
    private val mutableClientState = MutableStateFlow(CMClientState.Offline)
    val clientState = mutableClientState.asStateFlow()

    /**
     * Starts a coroutine which launches the WSS client. Also used to start the client if it's not started yet.
     * If not started, suspends the coroutine until the CMClient connects.
     */
    suspend fun tryConnect() = coroutineScope {
        launchConnectionCoroutine()
        awaitConnection(authRequired = false)
    }

    /**
     * Stops everything that is connected to a CM connection.
     *
     * You should not use this instance of [CMClient] after calling [stop].
     */
    fun stop() {
        internalScope.cancel()
    }

    private suspend fun awaitConnection(authRequired: Boolean = true) {
        clientState.first {
            if (authRequired) {
                it == CMClientState.Connected
            } else {
                it == CMClientState.AwaitingAuthorization || it == CMClientState.Authorizing || it == CMClientState.Connected
            }
        }
    }

    private fun launchConnectionCoroutine(reconnect: Boolean = false) {
        if (clientState.value != CMClientState.Offline) return

        internalScope.launch {
            if (reconnect) {
                mutableClientState.value = CMClientState.Reconnecting
                delay(1000L) // don't spam the server
            } else {
                mutableClientState.value = CMClientState.Connecting
            }

            connect()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun connect() {
        logger.logDebug("CMClient:Start") { "Fetching CMList" }
        selectedServer = serverList.getBestServer()

        logger.logDebug("CMClient:Start") { "Connecting to WSS [url = ${selectedServer?.endpoint}]" }
        httpClient.wss(urlString = "wss://" + (selectedServer?.endpoint ?: return) + "/cmsocket/") {
            call.request.attributes

            logger.logDebug("CMClient:WsConnection") { "Connected to Steam3 network" }

            outgoingPacketsQueue.send(
                SteamPacket.newProto(
                    messageId = EMsg.k_EMsgClientHello,
                    adapter = CMsgClientHello.ADAPTER,
                    payload = CMsgClientHello(protocol_version = EnvironmentConstants.PROTOCOL_VERSION)
                )
            )

            mutableClientState.value = CMClientState.AwaitingAuthorization

            while (true) {
                // Check if a message from server is present
                if (incoming.isEmpty.not()) {
                    val packetToReceive = incoming.receive()
                    if (packetToReceive is Frame.Binary) {
                        // Parse message out from this and add to queue

                        val steamPacket = runCatching {
                            SteamPacket.ofNetworkPacket(packetToReceive.data)
                        }

                        if (steamPacket.isSuccess) {
                            steamPacket.getOrNull()?.let { checkedPacket ->
                                if (checkedPacket.messageId == EMsg.k_EMsgMulti) {
                                    handleMultiPacket(checkedPacket)
                                } else {
                                    dumper.onPacket(checkedPacket, false)

                                    if (checkedPacket.messageId == EMsg.k_EMsgClientLogOnResponse) {
                                        handleClientLogOn(checkedPacket)
                                    }

                                    mutableIncomingPacketsQueue.emit(checkedPacket)
                                }
                            }
                        } else {
                            logger.logError("CMClient:WsConnection") {
                                "Error when receiving binary message: ${steamPacket.exceptionOrNull()?.message ?: "No exception provided"}"
                            }
                        }
                    } else {
                        logger.logDebug("CMClient:WsConnection") {
                            "Received non-binary message (type: ${packetToReceive.frameType.name})"
                        }
                    }
                }

                // Check if outgoing messages are in queue
                if (outgoingPacketsQueue.isEmpty.not()) {
                    val packetToSend = outgoingPacketsQueue.receive()

                    if (packetToSend.messageId == EMsg.k_EMsgClientLogon) {
                        mutableClientState.value = CMClientState.Authorizing
                    }

                    logger.logVerbose("CMClient:WsConnection") { "Sending packet: ${packetToSend.messageId.name}" }
                    logger.logVerbose("CMClient:WsConnection") { "> [header] ${packetToSend.header}" }
                    dumper.onPacket(packetToSend, true)
                    send(packetToSend.encode())
                }
            }
        }
    }

    private fun handleClientLogOn(checkedPacket: SteamPacket) {
        if (checkedPacket.success) {
            CMsgClientLogonResponse.ADAPTER.decode(checkedPacket.payload).also { payloadResult ->
                if (payloadResult.eresult != EResult.OK.encoded) {
                    return // Failed sign-in
                }

                cellId = payloadResult.cell_id ?: 0
                clientSteamId = SteamId(payloadResult.client_supplied_steamid?.toULong() ?: 0u)
                internalScope.startHeartbeat(intervalMs = (payloadResult.heartbeat_seconds ?: 9) * 1000L)
            }

            clientSessionId = checkedPacket.header.sessionId
            mutableClientState.value = CMClientState.Connected
        }
    }

    /**
     * Sometimes, Steam can send multi-messages (gzipped 2+ messages at once).
     * This function handles such messages.
     */
    private suspend fun handleMultiPacket(checkedPacket: SteamPacket) {
        val payload = CMsgMulti.ADAPTER.decode(checkedPacket.payload)

        if ((payload.size_unzipped ?: 0) > 0) {
            logger.logVerbose("SteamPacket:Multi") {
                "Parsing multi-message (compressed size: ${payload.size_unzipped} bytes)"
            }
        } else {
            logger.logVerbose("SteamPacket:Multi") {
                "Parsing multi-message (no compressed data)"
            }
        }

        require(payload.message_body != null) { "Payload body is null" }

        val payloadBuffer = Buffer().write(payload.message_body ?: return).let {
            if ((payload.size_unzipped ?: 0) > 0) {
                (it as Source).gzip().buffer()
            } else {
                it
            }
        }

        do {
            val packetSize = payloadBuffer.readIntLe()
            val packetContent = payloadBuffer.readByteArray(packetSize.toLong())
            val packetParsed = SteamPacket.ofNetworkPacket(packetContent)

            dumper.onPacket(packetParsed, false)

            if (packetParsed.messageId == EMsg.k_EMsgClientLogOnResponse) {
                handleClientLogOn(packetParsed)
            }

            mutableIncomingPacketsQueue.emit(packetParsed)
        } while (payloadBuffer.exhausted().not())
    }

    /**
     * Add a packet to an outgoing queue and then awaits for a response with the attached job ID.
     *
     * @param packet the packet which needs to be queued for sending
     */
    suspend fun execute(packet: SteamPacket): SteamPacket {
        return subscribe(packet).first()
    }

    /**
     * Add a packet to an outgoing queue and then awaits for a (multiple) responses with the attached job ID.
     *
     * @param packet the packet which needs to be queued for sending
     * @return a [Flow] of [SteamPacket]s related to this packet
     */
    suspend fun subscribe(packet: SteamPacket): Flow<SteamPacket> {
        awaitConnection(authRequired = SteamPacket.canBeExecutedWithoutAuth(packet).not())

        val processedSourcePacket = packet.enrichWithClientData().apply {
            header.sourceJobId = ++jobIdCounter
        }

        return incomingPacketsQueue.onSubscription {
            outgoingPacketsQueue.send(processedSourcePacket)
        }.filter { incomingPacket ->
            incomingPacket.header.targetJobId == processedSourcePacket.header.sourceJobId
        }
    }

    /**
     * Add a packet to an outgoing queue and forget about it (no job IDs and awaits)
     *
     * @param packet the packet which needs to be queued for sending
     */
    suspend fun executeAndForget(packet: SteamPacket) {
        awaitConnection(authRequired = SteamPacket.canBeExecutedWithoutAuth(packet).not())
        outgoingPacketsQueue.send(packet.enrichWithClientData())
    }

    private fun SteamPacket.enrichWithClientData() = apply {
        header.sessionId = clientSessionId

        if (header.steamId == SteamId.Empty.id && clientSteamId.id != SteamId.Empty.id) {
            header.steamId = clientSteamId.id
        }
    }

    private fun CoroutineScope.startHeartbeat(intervalMs: Long) {
        val actorJob = Job()

        launch(actorJob + CoroutineName("kSteam-heartbeat")) {
            while (true) {
                logger.logVerbose("CMClient:Heartbeat") { "Adding heartbeat packet to queue" }

                executeAndForget(
                    SteamPacket.newProto(
                        messageId = EMsg.k_EMsgClientHeartBeat,
                        adapter = CMsgClientHeartBeat.ADAPTER,
                        payload = CMsgClientHeartBeat()
                    )
                )

                delay(intervalMs)
            }
        }

        coroutineContext[Job]?.invokeOnCompletion {
            actorJob.cancel()
        }
    }
}