package bruhcollective.itaysonlab.ksteam.network

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.SteamClientConfiguration
import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.debug.PacketDumper
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.platform.CreateSupervisedCoroutineScope
import bruhcollective.itaysonlab.ksteam.web.models.CMServerEntry
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

internal class CMClient(
    private val serverList: CMList,
    private val configuration: SteamClientConfiguration
) {
    // A scope used to hold WSS connection
    private val internalScope = CreateSupervisedCoroutineScope("cmClient", Dispatchers.IO) { _, throwable ->
        throwable.printStackTrace()
        mutableClientState.value = CMClientState.Idle
        launchConnectionCoroutine(reconnect = true)
    }

    private var selectedServer: CMServerEntry? = null

    /**
     * A counter used to set sourceJobID field in the Steam packets.
     */
    private var jobIdCounter = 0L

    private var cellId = 0
    private var clientSessionId = 0
    internal var clientSteamId = SteamId.Empty

    internal val dumper = PacketDumper(configuration.rootFolder)

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
    private val mutableClientState = MutableStateFlow(CMClientState.Idle)
    val clientState = mutableClientState.asStateFlow()

    /**
     * Starts a coroutine which launches the WSS client. Also used to start the client if it's not started yet.
     * If not started, suspends the coroutine until the CMClient connects.
     */
    suspend fun tryConnect() = coroutineScope {
        launchConnectionCoroutine()
        awaitConnection(authRequired = false)
    }

    private suspend fun awaitConnection(authRequired: Boolean = true) {
        clientState.first {
            if (authRequired) {
                it == CMClientState.Connected
            } else {
                it == CMClientState.Logging || it == CMClientState.Connected
            }
        }
    }

    private fun launchConnectionCoroutine(reconnect: Boolean = false) {
        if (clientState.value != CMClientState.Idle) return

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
        KSteamLogging.logDebug("CMClient:Start", "Fetching CMList")
        selectedServer = serverList.getBestServer()

        KSteamLogging.logDebug("CMClient:Start", "Connecting to WSS [url = ${selectedServer?.endpoint}]")
        configuration.networkClient.wss(urlString = "wss://" + (selectedServer?.endpoint ?: return) + "/cmsocket/") {
            call.request.attributes

            KSteamLogging.logDebug("CMClient:WsConnection", "Connected to Steam3 network")

            send(
                Frame.Binary(
                    fin = true,
                    data = SteamPacket.newProto(
                        messageId = EMsg.k_EMsgClientHello,
                        adapter = CMsgClientHello.ADAPTER,
                        payload = CMsgClientHello(protocol_version = EnvironmentConstants.PROTOCOL_VERSION)
                    ).encode()
                )
            )

            mutableClientState.value = CMClientState.Logging

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
                            KSteamLogging.logError(
                                "CMClient:WsConnection",
                                "Error when receiving binary message: ${steamPacket.exceptionOrNull()?.message ?: "No exception provided"}"
                            )
                        }
                    } else {
                        KSteamLogging.logDebug(
                            "CMClient:WsConnection",
                            "Received non-binary message (type: ${packetToReceive.frameType.name})"
                        )
                    }
                }

                // Check if outgoing messages are in queue
                if (outgoingPacketsQueue.isEmpty.not()) {
                    val packetToSend = outgoingPacketsQueue.receive()
                    KSteamLogging.logVerbose("CMClient:WsConnection", "Sending packet: ${packetToSend.messageId.name}")
                    KSteamLogging.logVerbose("CMClient:WsConnection", "> [header] ${packetToSend.header}")
                    dumper.onPacket(packetToSend, true)
                    send(packetToSend.encode())
                }
            }
        }
    }

    private fun handleClientLogOn(checkedPacket: SteamPacket) {
        val payloadResult = checkedPacket.getProtoPayload(CMsgClientLogonResponse.ADAPTER)

        if (payloadResult.data.eresult == EResult.OK.encoded) {
            cellId = payloadResult.data.cell_id ?: 0
            clientSessionId = checkedPacket.header.sessionId
            clientSteamId = SteamId(payloadResult.data.client_supplied_steamid?.toULong() ?: 0u)
            internalScope.startHeartbeat(intervalMs = (payloadResult.data.heartbeat_seconds ?: 9) * 1000L)
            mutableClientState.value = CMClientState.Connected
        }
    }

    /**
     * Sometimes, Steam can send multi-messages (gzipped 2+ messages at once).
     * This function handles such messages.
     */
    private suspend fun handleMultiPacket(checkedPacket: SteamPacket) {
        val payloadResult = checkedPacket.getProtoPayload(CMsgMulti.ADAPTER)

        if (payloadResult.hasData) {
            val payload = payloadResult.data

            if ((payload.size_unzipped ?: 0) > 0) {
                KSteamLogging.logVerbose(
                    "SteamPacket:Multi",
                    "Parsing multi-message (compressed size: ${payload.size_unzipped} bytes)"
                )
            } else {
                KSteamLogging.logVerbose("SteamPacket:Multi", "Parsing multi-message (no compressed data)")
            }

            require(payload.message_body != null) { "Payload body is null" }

            val payloadBuffer = if ((payload.size_unzipped ?: 0) > 0) {
                (Buffer().write(payload.message_body ?: return) as Source).gzip().buffer()
            } else {
                Buffer().write(payload.message_body ?: return)
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
        } else {
            KSteamLogging.logVerbose("SteamPacket:Multi", "> ${payloadResult.result.name}")
        }
    }

    /**
     * Add a packet to a outgoing queue and then awaits for a response with the attached job ID.
     */
    suspend fun execute(packet: SteamPacket): SteamPacket {
        return subscribe(packet).first()
    }

    /**
     * Add a packet to a outgoing queue and then awaits for a (multiple) responses with the attached job ID.
     */
    suspend fun subscribe(packet: SteamPacket): Flow<SteamPacket> {
        awaitConnection(authRequired = SteamPacket.canBeExecutedWithoutAuth(packet).not())

        val processedSourcePacket = packet.enrichWithClientData().apply {
            header.sourceJobId = ++jobIdCounter
        }

        return incomingPacketsQueue.onSubscription {
            outgoingPacketsQueue.trySend(processedSourcePacket)
        }.filter { incomingPacket ->
            incomingPacket.header.targetJobId == processedSourcePacket.header.sourceJobId
        }
    }

    /**
     * Add a packet to a outgoing queue and forget about it (no job IDs and awaits)
     */
    suspend fun executeAndForget(packet: SteamPacket) {
        awaitConnection(authRequired = SteamPacket.canBeExecutedWithoutAuth(packet).not())
        outgoingPacketsQueue.trySend(packet.enrichWithClientData())
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
                KSteamLogging.logVerbose("CMClient:Heartbeat", "Adding heartbeat packet to queue")

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