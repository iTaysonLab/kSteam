package bruhcollective.itaysonlab.ksteam.network

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import bruhcollective.itaysonlab.ksteam.debug.PacketDumper
import bruhcollective.itaysonlab.ksteam.handlers.Logger
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.messages.SteamPacketHeader
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import bruhcollective.itaysonlab.ksteam.network.event.IncomingPacketManager
import bruhcollective.itaysonlab.ksteam.network.exception.CMJobDroppedException
import bruhcollective.itaysonlab.ksteam.network.exception.CMJobTimeoutException
import bruhcollective.itaysonlab.ksteam.platform.ConnectivityStateDelayer
import bruhcollective.itaysonlab.ksteam.util.CreateSupervisedCoroutineScope
import com.squareup.wire.ProtoAdapter
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import okio.Buffer
import okio.Source
import okio.buffer
import okio.gzip
import steam.messages.clientserver_login.CMsgClientHello
import steam.webui.common.CMsgClientHeartBeat
import steam.webui.common.CMsgClientLogonResponse
import steam.webui.common.CMsgMulti
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

/**
 * This class manages connection to the CM network on Steam.
 */
internal class CMClient(
    private val serverList: CMList,
    private val dumper: PacketDumper,
    private val logger: Logger,
    private val httpClient: HttpClient,
    private val connectivityStateDelayer: ConnectivityStateDelayer,
    dispatcher: CoroutineDispatcher
) {
    /**
     * A scope used to hold WSS connection.
     */
    private val internalScope = CreateSupervisedCoroutineScope("CMClient", dispatcher)

    /**
     * A job manager for CM clients.
     */
    private val jobManager = CMJobManager(logger)

    /**
     * Incoming packet manager that automatically notifies typed listeners about incoming events.
     */
    internal val incomingPacketManager = IncomingPacketManager(logger)

    /**
     * Reference of the current WebSocket session.
     */
    private val wsSessionReference = atomic<WebSocketSession?>(null)

    private var cellId = 0
    internal var clientSessionId = 0
    internal var clientSteamId = SteamId.Empty

    /**
     * CMClient state
     */
    private val mutableClientState = MutableStateFlow(CMClientState.Offline)
    val clientState = mutableClientState.asStateFlow()

    /**
     * Heartbeat state
     */
    private var heartbeatJob: Job? = null

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

    private suspend fun awaitConnectionOrThrow(packet: SteamPacket, authRequired: Boolean) {
        withTimeoutOrNull(15.seconds) {
            awaitConnection(authRequired)
        } ?: throw CMJobTimeoutException(createJoblessInformation(packet))
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

    private fun launchConnectionCoroutine() {
        if (clientState.value != CMClientState.Offline) return

        internalScope.launch {
            connect()
        }
    }

    private suspend fun connect() {
        val selectedServerEndpoint = serverList.getEndpoint()

        try {
            connect(selectedServerEndpoint)
        } catch (e: Exception) {
            e.printStackTrace()
            wsSessionReference.getAndSet(null)
            jobManager.dropAllJobs(CMJobDroppedException.Reason.WsConnectionDropped)

            if (coroutineContext.isActive) {
                delay(1000L)
                connectivityStateDelayer.awaitUntilInternetConnection()
                connect()
            }
        } finally {
            wsSessionReference.getAndSet(null)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun connect(endpoint: String) {
        heartbeatJob?.cancel()

        mutableClientState.value = CMClientState.Connecting
        logger.logDebug("CMClient:Start") { "Connecting to WSS [url = ${endpoint}]" }

        httpClient.wss(urlString = "wss://$endpoint/cmsocket/") {
            logger.logDebug("CMClient:WsConnection") { "Connected to Steam3 network" }

            wsSessionReference.getAndSet(this)

            send(
                SteamPacket.newProto(
                    messageId = EMsg.k_EMsgClientHello,
                    payload = CMsgClientHello(protocol_version = EnvironmentConstants.PROTOCOL_VERSION)
                ).encode()
            )

            mutableClientState.value = CMClientState.AwaitingAuthorization

            while (currentCoroutineContext().isActive) {
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

                                handleIncomingPacket(checkedPacket)
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
        }
    }

    private fun handleClientLogOn(checkedPacket: SteamPacket) {
        if (checkedPacket.error) return

        CMsgClientLogonResponse.ADAPTER.decode(checkedPacket.payload).also { payloadResult ->
            if (payloadResult.eresult == EResult.Expired.encoded || payloadResult.eresult == EResult.AccessDenied.encoded) {
                // Account will automatically delete it and restart CMClient
                return // Expired session
            } else if (payloadResult.eresult != EResult.OK.encoded) {
                return // Failed sign-in
            }

            cellId = payloadResult.cell_id ?: 0
            clientSteamId = SteamId(payloadResult.client_supplied_steamid?.toULong() ?: 0u)
            internalScope.startHeartbeat(intervalMs = (payloadResult.heartbeat_seconds ?: 9) * 1000L)
        }

        clientSessionId = checkedPacket.header.sessionId
        mutableClientState.value = CMClientState.Connected
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

            handleIncomingPacket(packetParsed)
        } while (payloadBuffer.exhausted().not())
    }

    /**
     * Sends a [SteamPacket] without expecting a response.
     */
    suspend fun execute(
        packet: SteamPacket
    ) {
        wsSessionReference.value?.let { wsSession ->
            dumper.onPacket(packet, true)
            wsSession.send(packet.addClientData().encode())
        }
    }

    /**
     * Sends a [SteamPacket] and awaits a single [SteamPacket].
     */
    suspend fun executeSingle(
        packet: SteamPacket
    ): SteamPacket {
        awaitConnectionOrThrow(packet = packet, authRequired = SteamPacket.canBeExecutedWithoutAuth(packet).not())
        return postJob(packet, CMJob.Single(createJobInformation(packet))).await()
    }

    /**
     * Sends a [SteamPacket] and awaits a single protobuf message.
     */
    suspend fun <T> executeSingleProtobuf(
        packet: SteamPacket,
        adapter: ProtoAdapter<T>
    ): T {
        awaitConnectionOrThrow(packet = packet, authRequired = SteamPacket.canBeExecutedWithoutAuth(packet).not())
        return postJob(packet, CMJob.SingleProtobuf(createJobInformation(packet), adapter)).await()
    }

    /**
     * Sends a [SteamPacket] and awaits multiple protobuf messages.
     */
    suspend fun <T> executeMultipleProtobuf(
        packet: SteamPacket,
        adapter: ProtoAdapter<T>,
        stopIf: (T) -> Boolean
    ): List<T> {
        awaitConnectionOrThrow(packet = packet, authRequired = SteamPacket.canBeExecutedWithoutAuth(packet).not())
        return postJob(packet, CMJob.MultipleProtobuf(createJobInformation(packet), adapter, stopIf)).await()
    }

    /**
     * Sends a [SteamPacket] and awaits multiple protobuf messages.
     */
    suspend fun <T> executeMultipleStreamingProtobuf(
        packet: SteamPacket,
        adapter: ProtoAdapter<T>,
        process: suspend (T) -> Boolean
    ) {
        awaitConnectionOrThrow(packet = packet, authRequired = SteamPacket.canBeExecutedWithoutAuth(packet).not())
        return postJob(packet, CMJob.MultipleStreamingProtobuf(createJobInformation(packet), adapter, process)).await()
    }

    private fun createJobInformation(from: SteamPacket): CMJobInformation {
        return CMJobInformation(
            id = jobManager.createJobId(),
            name = (from.header as? SteamPacketHeader.Protobuf)?.targetJobName.orEmpty(),
            msgId = from.messageId
        )
    }

    private fun createJoblessInformation(from: SteamPacket): CMJobInformation {
        return CMJobInformation(
            id = -1,
            name = (from.header as? SteamPacketHeader.Protobuf)?.targetJobName.orEmpty(),
            msgId = from.messageId
        )
    }

    private suspend fun handleIncomingPacket(packet: SteamPacket) {
        val isPacketAttachedToAJob = jobManager.completeJob(packet)
        if (isPacketAttachedToAJob) return

        incomingPacketManager.handleIncomingPacket(packet)
    }

    private suspend fun <T> postJob(packet: SteamPacket, job: CMJob<T>): Deferred<T> {
        val wsSession = wsSessionReference.value

        packet.addClientData().also {
            it.header.sourceJobId = job.information.id
        }

        if (wsSession != null) {
            if (packet.messageId == EMsg.k_EMsgClientLogon) mutableClientState.value = CMClientState.Authorizing
            dumper.onPacket(packet, true)

            logger.logVerbose("CMClient") { "posting ${job.information}" }

            jobManager.postJob(job)
            wsSession.send(packet.encode())
        } else {
            job.failDropped(CMJobDroppedException.Reason.WsSessionUnavailable)
        }

        return job.deferred
    }

    private fun SteamPacket.addClientData() = apply {
        header.sessionId = clientSessionId

        if (header.steamId == SteamId.Empty.id && clientSteamId.id != SteamId.Empty.id) {
            header.steamId = clientSteamId.id
        }
    }

    private fun CoroutineScope.startHeartbeat(intervalMs: Long) {
        heartbeatJob?.cancel()

        val actorJob = Job()

        heartbeatJob = launch(actorJob + CoroutineName("kSteam-heartbeat")) {
            while (currentCoroutineContext().isActive) {
                logger.logVerbose("CMClient:Heartbeat") { "Adding heartbeat packet to queue" }

                runCatching {
                    wsSessionReference.value?.send(
                        SteamPacket.newProto(
                            messageId = EMsg.k_EMsgClientHeartBeat,
                            payload = CMsgClientHeartBeat()
                        ).encode()
                    )
                }.onFailure {
                    logger.logWarning("CMClient:Heartbeat") { "Failed to send heartbeat: ${it.message}" }
                }

                delay(intervalMs)
            }
        }

        coroutineContext[Job]?.invokeOnCompletion {
            actorJob.cancel()
        }
    }
}