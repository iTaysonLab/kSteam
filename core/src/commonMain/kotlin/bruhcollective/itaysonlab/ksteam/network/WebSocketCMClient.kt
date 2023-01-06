package bruhcollective.itaysonlab.ksteam.network

import bruhcollective.itaysonlab.ksteam.SteamClientConfiguration
import bruhcollective.itaysonlab.ksteam.debug.logDebug
import bruhcollective.itaysonlab.ksteam.messages.BasePacketMessage
import bruhcollective.itaysonlab.ksteam.messages.ProtoPacketMessage
import bruhcollective.itaysonlab.ksteam.models.JobId
import bruhcollective.itaysonlab.ksteam.util.MultiplatformIODispatcher
import bruhcollective.itaysonlab.ksteam.web.models.CMServerEntry
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import steam.enums.EMsg
import steam.messages.clientserver_login.CMsgClientHeartBeat
import steam.messages.clientserver_login.CMsgClientHello
import kotlin.time.Duration.Companion.seconds

internal class WebSocketCMClient (
    private val serverList: CMList,
    private val configuration: SteamClientConfiguration
) {
    // A scope used to hold WSS connection
    private val internalScope = CoroutineScope(MultiplatformIODispatcher + SupervisorJob() + CoroutineName("kSteam-cmClient") + CoroutineExceptionHandler { coroutineContext, throwable ->
        logDebug("SteamClient:Scope", "WSS error: ${throwable.message}")
        // TODO should delay and restart
        // launchConnectionCoroutine()
    })

    private var selectedServer: CMServerEntry? = null

    /**
     * A counter used to set sourceJobID field in the Steam packets.
     */
    private var jobIdCounter = 0L

    /**
     * A queue for outgoing packets. These will be collected in the WebSocket loop and sent to the server.
     */
    private val outgoingPacketsQueue = Channel<BasePacketMessage<*>>(capacity = Channel.UNLIMITED)

    /**
     * A queue for incoming packets, which are processed by consumers
     */
    private val mutableIncomingPacketsQueue = MutableSharedFlow<BasePacketMessage<*>>(0, Int.MAX_VALUE, BufferOverflow.SUSPEND)
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

        logDebug("SteamClient:Start", "Fetching CMList")
        selectedServer = serverList.getBestServer()

        logDebug("SteamClient:Start", "Connecting to WSS [url = ${selectedServer?.endpoint}]")
        configuration.networkClient.wss(urlString = "wss://" + (selectedServer?.endpoint ?: return) + "/cmsocket/") {
            logDebug("SteamClient:WsConnection", "Connected, sending hello (and login if available)")

            send(Frame.Binary(fin = true, ProtoPacketMessage(
                messageId = EMsg.k_EMsgClientHello,
                protobufAdapter = CMsgClientHello.ADAPTER
            ).withPayload(CMsgClientHello(
                protocol_version = 65580
            )).toSteamPacket()))

            logDebug("SteamClient:WsConnection", "Hello sent successfully, now starting the loop")

            clientState.value = CMClientState.Connected

            while (true) {
                if (incoming.isEmpty.not() || outgoingPacketsQueue.isEmpty.not()) {
                    logDebug("SteamClient:WsConnection", "Loop started (have incoming: ${incoming.isEmpty.not()}, have outgoing: ${outgoingPacketsQueue.isEmpty.not()})")
                }

                // Check if a message from server is present
                if (incoming.isEmpty.not()) {
                    val packetToReceive = incoming.receive()
                    if (packetToReceive is Frame.Binary) {
                        logDebug("SteamClient:WsConnection", "Received binary message (data: ${packetToReceive.data})")
                        // Parse message out from this and add to queue
                    } else {
                        logDebug("SteamClient:WsConnection", "Received non-binary message (type: ${packetToReceive.frameType.name})")
                    }
                }

                // Check if outgoing messages are in queue
                if (outgoingPacketsQueue.isEmpty.not()) {
                    val packetToSend = outgoingPacketsQueue.receive()
                    logDebug("SteamClient:WsConnection", "Sending packet to Steam3 (message: ${packetToSend.messageId.name})")
                    send(Frame.Binary(fin = true, data = packetToSend.toSteamPacket()))
                }
            }
        }
    }

    /**
     * Add a packet of a type <Request> to a outgoing queue and then awaits for a response with a type <Response>.
     * Use this only for typed requests with known request/results. If
     */
    suspend fun <Request, Response> execute(packet: BasePacketMessage<Request>): BasePacketMessage<Response> {
        tryConnect()

        val processedSourcePacket = packet.apply {
            sourceJobId = JobId(++jobIdCounter)
        }

        return incomingPacketsQueue.onSubscription {
            outgoingPacketsQueue.trySend(processedSourcePacket)
        }.filter { incomingPacket ->
            incomingPacket.targetJobId == processedSourcePacket.sourceJobId
        }.filterIsInstance<BasePacketMessage<Response>>().single()
    }

    /**
     * Add a packet to a outgoing queue and forget about it (no job IDs and awaits)
     */
    suspend fun executeAndForget(packet: BasePacketMessage<*>) {
        tryConnect()
        outgoingPacketsQueue.trySend(packet)
    }

    private fun CoroutineScope.startHeartbeat(intervalMs: Long) {
        val actorJob = Job()

        launch(actorJob + CoroutineName("kSteam-heartbeat")) {
            while (true) {
                logDebug("SteamClient:Heartbeat", "Adding heartbeat packet to queue")

                executeAndForget(ProtoPacketMessage(
                    messageId = EMsg.k_EMsgClientHeartBeat,
                    protobufAdapter = CMsgClientHeartBeat.ADAPTER
                ).withPayload(CMsgClientHeartBeat()))

                delay(intervalMs)
            }
        }

        coroutineContext[Job]?.invokeOnCompletion {
            actorJob.cancel()
        }
    }
}