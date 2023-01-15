package bruhcollective.itaysonlab.ksteam.network

import bruhcollective.itaysonlab.ksteam.SteamClientConfiguration
import bruhcollective.itaysonlab.ksteam.debug.logDebug
import bruhcollective.itaysonlab.ksteam.debug.logError
import bruhcollective.itaysonlab.ksteam.debug.logVerbose
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.platform.CreateSupervisedCoroutineScope
import bruhcollective.itaysonlab.ksteam.util.send
import bruhcollective.itaysonlab.ksteam.web.models.CMServerEntry
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import okio.Buffer
import okio.ByteString.Companion.toByteString
import okio.Source
import okio.buffer
import okio.gzip
import steam.messages.base.CMsgMulti
import steam.messages.clientserver_login.CMsgClientHeartBeat
import steam.messages.clientserver_login.CMsgClientHello
import steam.messages.clientserver_login.CMsgClientLogonResponse

internal class CMClient (
    private val serverList: CMList,
    private val configuration: SteamClientConfiguration
) {
    // A scope used to hold WSS connection
    private val internalScope = CreateSupervisedCoroutineScope("cmClient", Dispatchers.IO) { ctx, throwable ->
        logError("CMClient:Restarter", "WSS error: ${throwable.message} <exception: ${throwable::class.simpleName}>")
        mutableClientState.value = CMClientState.Connecting
        // TODO: restart
    }

    private var selectedServer: CMServerEntry? = null

    /**
     * A counter used to set sourceJobID field in the Steam packets.
     */
    private var jobIdCounter = 0L

    private var cellId = 0
    private var clientSessionId = 0

    /**
     * A queue for outgoing packets. These will be collected in the WebSocket loop and sent to the server.
     */
    private val outgoingPacketsQueue = Channel<SteamPacket>(capacity = Channel.UNLIMITED)

    /**
     * A queue for incoming packets, which are processed by consumers
     */
    private val mutableIncomingPacketsQueue = MutableSharedFlow<SteamPacket>(0, Int.MAX_VALUE, BufferOverflow.SUSPEND)
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

        clientState.first {
            it == CMClientState.Connected
        }
    }

    private fun launchConnectionCoroutine() {
        if (clientState.value != CMClientState.Idle) return

        internalScope.launch {
            connect()
        }.invokeOnCompletion {
            mutableClientState.value = CMClientState.Idle
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun connect() {
        mutableClientState.value = CMClientState.Connecting

        logDebug("CMClient:Start", "Fetching CMList")
        selectedServer = serverList.getBestServer()

        logDebug("CMClient:Start", "Connecting to WSS [url = ${selectedServer?.endpoint}]")
        configuration.networkClient.wss(urlString = "wss://" + (selectedServer?.endpoint ?: return) + "/cmsocket/") {
            call.request.attributes

            logDebug("CMClient:WsConnection", "Connected to Steam3 network")

            send(
                SteamPacket.newProto(
                        messageId = EMsg.k_EMsgClientHello,
                        adapter = CMsgClientHello.ADAPTER,
                        payload = CMsgClientHello(protocol_version = 65580)
                )
            )

            mutableClientState.value = CMClientState.Logging

            while (true) {
                // Check if a message from server is present
                if (incoming.isEmpty.not()) {
                    val packetToReceive = incoming.receive()
                    if (packetToReceive is Frame.Binary) {
                        logVerbose("CMClient:WsConnection", "Received binary message (data: ${packetToReceive.data.toByteString().hex()})")
                        // Parse message out from this and add to queue

                        val steamPacket = runCatching {
                            SteamPacket.ofNetworkPacket(packetToReceive.data)
                        }

                        if (steamPacket.isSuccess) {
                            steamPacket.getOrNull()?.let { checkedPacket ->
                                if (checkedPacket.messageId == EMsg.k_EMsgMulti) {
                                    handleMultiPacket(checkedPacket)
                                } else {
                                    if (checkedPacket.messageId == EMsg.k_EMsgClientLogOnResponse) {
                                        handleClientLogOn(checkedPacket)
                                    }

                                    mutableIncomingPacketsQueue.tryEmit(checkedPacket)
                                }
                            }
                        } else {
                            logError("CMClient:WsConnection", "Error when receiving binary message: ${steamPacket.exceptionOrNull()?.message ?: "No exception provided"}")
                        }
                    } else {
                        logDebug("CMClient:WsConnection", "Received non-binary message (type: ${packetToReceive.frameType.name})")
                    }
                }

                // Check if outgoing messages are in queue
                if (outgoingPacketsQueue.isEmpty.not()) {
                    val packetToSend = outgoingPacketsQueue.receive()
                    logVerbose("CMClient:WsConnection", "Sending packet: ${packetToSend.messageId.name}")
                    send(packetToSend.encode())
                }
            }
        }
    }

    private fun handleClientLogOn(checkedPacket: SteamPacket) {
        val payloadResult = checkedPacket.getProtoPayload(CMsgClientLogonResponse.ADAPTER)

        if (payloadResult.isSuccess && payloadResult.data.eresult == EResult.OK.encoded) {
            cellId = payloadResult.data.cell_id ?: 0
            clientSessionId = checkedPacket.header.sessionId
            internalScope.startHeartbeat(intervalMs = (payloadResult.data.heartbeat_seconds ?: 9) * 1000L)
            mutableClientState.value = CMClientState.Connected
        }
    }

    /**
     * Sometimes, Steam can send multi-messages (gzipped 2+ messages at once).
     * This function handles such messages.
     */
    private fun handleMultiPacket(checkedPacket: SteamPacket) {
        val payloadResult = checkedPacket.getProtoPayload(CMsgMulti.ADAPTER)

        if (payloadResult.hasData) {
            val payload = payloadResult.data

            if ((payload.size_unzipped ?: 0) > 0) {
                logVerbose("SteamPacket:Multi", "Parsing multi-message (compressed size: ${payload.size_unzipped} bytes)")
            } else {
                logVerbose("SteamPacket:Multi", "Parsing multi-message (no compressed data)")
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
                logVerbose("SteamPacket:Multi", "> ${packetContent.toByteString().hex()}")
                mutableIncomingPacketsQueue.tryEmit(SteamPacket.ofNetworkPacket(packetContent))
            } while (payloadBuffer.exhausted().not())
        } else {
            logVerbose("SteamPacket:Multi", "> ${payloadResult.result.name}")
        }
    }

    /**
     * Add a packet to a outgoing queue and then awaits for a response with the attached job ID.
     */
    suspend fun execute(packet: SteamPacket): SteamPacket {
        tryConnect()

        val processedSourcePacket = packet.apply {
            header.sourceJobId = ++jobIdCounter
            header.sessionId = clientSessionId
        }

        return incomingPacketsQueue.onSubscription {
            outgoingPacketsQueue.trySend(processedSourcePacket)
        }.filter { incomingPacket ->
            incomingPacket.header.targetJobId == processedSourcePacket.header.sourceJobId
        }.first()
    }

    /**
     * Add a packet to a outgoing queue and forget about it (no job IDs and awaits)
     */
    suspend fun executeAndForget(packet: SteamPacket) {
        tryConnect()
        outgoingPacketsQueue.trySend(packet)
    }

    private fun CoroutineScope.startHeartbeat(intervalMs: Long) {
        val actorJob = Job()

        launch(actorJob + CoroutineName("kSteam-heartbeat")) {
            while (true) {
                logVerbose("CMClient:Heartbeat", "Adding heartbeat packet to queue")

                executeAndForget(SteamPacket.newProto(
                    messageId = EMsg.k_EMsgClientHeartBeat,
                    adapter = CMsgClientHeartBeat.ADAPTER,
                    payload = CMsgClientHeartBeat()
                ))

                delay(intervalMs)
            }
        }

        coroutineContext[Job]?.invokeOnCompletion {
            actorJob.cancel()
        }
    }
}