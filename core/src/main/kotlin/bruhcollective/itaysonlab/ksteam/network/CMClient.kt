package bruhcollective.itaysonlab.ksteam.network

import bruhcollective.itaysonlab.ksteam.SteamClientConfiguration
import bruhcollective.itaysonlab.ksteam.debug.logDebug
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
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
import steam.enums.EMsg
import steam.messages.base.CMsgMulti
import steam.messages.clientserver_login.CMsgClientHeartBeat
import steam.messages.clientserver_login.CMsgClientHello

internal class CMClient (
    private val serverList: CMList,
    private val configuration: SteamClientConfiguration
) {
    // A scope used to hold WSS connection
    private val internalScope = CreateSupervisedCoroutineScope("cmClient", Dispatchers.IO) { _, throwable ->
        logDebug("CMClient:Restarter", "WSS error: ${throwable.message} <exception: ${throwable::class.simpleName}>")
        // TODO should delay and restart
        // launchConnectionCoroutine()
    }

    private var selectedServer: CMServerEntry? = null

    /**
     * A counter used to set sourceJobID field in the Steam packets.
     */
    private var jobIdCounter = 0L

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
    val clientState = MutableStateFlow(CMClientState.Idle)

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
            clientState.value = CMClientState.Idle
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun connect() {
        clientState.value = CMClientState.Connecting

        logDebug("CMClient:Start", "Fetching CMList")
        selectedServer = serverList.getBestServer()

        logDebug("CMClient:Start", "Connecting to WSS [url = ${selectedServer?.endpoint}]")
        configuration.networkClient.wss(urlString = "wss://" + (selectedServer?.endpoint ?: return) + "/cmsocket/") {
            logDebug("CMClient:WsConnection", "Connected, sending hello (and login if available)")

            send(
                SteamPacket.newProto(
                        messageId = EMsg.k_EMsgClientHello,
                        adapter = CMsgClientHello.ADAPTER,
                        payload = CMsgClientHello(protocol_version = 65580)
                )
            )

            logDebug("CMClient:WsConnection", "Hello sent successfully, now starting the loop")

            clientState.value = CMClientState.Connected

            while (true) {
                if (incoming.isEmpty.not() || outgoingPacketsQueue.isEmpty.not()) {
                    logDebug("CMClient:WsConnection", "Loop started (have incoming: ${incoming.isEmpty.not()}, have outgoing: ${outgoingPacketsQueue.isEmpty.not()})")
                }

                // Check if a message from server is present
                if (incoming.isEmpty.not()) {
                    val packetToReceive = incoming.receive()
                    if (packetToReceive is Frame.Binary) {
                        logDebug("CMClient:WsConnection", "Received binary message (data: ${packetToReceive.data.toByteString().hex()})")
                        // Parse message out from this and add to queue

                        val steamPacket = runCatching {
                            SteamPacket.ofNetworkPacket(packetToReceive.data)
                        }

                        if (steamPacket.isSuccess) {
                            steamPacket.getOrNull()?.let { checkedPacket ->
                                if (checkedPacket.messageId == EMsg.k_EMsgMulti) {
                                    handleMultiPacket(checkedPacket)
                                } else {
                                    mutableIncomingPacketsQueue.tryEmit(checkedPacket)
                                }
                            }
                        } else {
                            logDebug("CMClient:WsConnection", "Error when receiving binary message: ${steamPacket.exceptionOrNull()?.message ?: "No exception provided"}")
                        }
                    } else {
                        logDebug("CMClient:WsConnection", "Received non-binary message (type: ${packetToReceive.frameType.name})")
                    }
                }

                // Check if outgoing messages are in queue
                if (outgoingPacketsQueue.isEmpty.not()) {
                    val packetToSend = outgoingPacketsQueue.receive()
                    logDebug("CMClient:WsConnection", "Sending packet to Steam3 (message: ${packetToSend.messageId.name})")
                    send(packetToSend.encode().also {
                        logDebug("CMClient:WsConnection", "> ${it.toByteString().hex()}")
                    })
                }
            }
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
                logDebug("SteamPacket:Multi", "Parsing multi-message (compressed size: ${payload.size_unzipped} bytes)")
            } else {
                logDebug("SteamPacket:Multi", "Parsing multi-message (no compressed data)")
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
                logDebug("SteamPacket:Multi", "> ${packetContent.toByteString().hex()}")
                mutableIncomingPacketsQueue.tryEmit(SteamPacket.ofNetworkPacket(packetContent))
            } while (payloadBuffer.exhausted().not())
        } else {
            logDebug("SteamPacket:Multi", "> ${payloadResult.result.name}")
        }
    }

    /**
     * Add a packet to a outgoing queue and then awaits for a response with the attached job ID.
     */
    suspend fun execute(packet: SteamPacket): SteamPacket {
        tryConnect()

        val processedSourcePacket = packet.apply {
            header.sourceJobId = ++jobIdCounter
            header.sessionId = 0
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
                logDebug("CMClient:Heartbeat", "Adding heartbeat packet to queue")

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