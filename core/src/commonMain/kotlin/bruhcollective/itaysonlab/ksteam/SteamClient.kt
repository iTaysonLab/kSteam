package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.debug.PacketDumper
import bruhcollective.itaysonlab.ksteam.grpc.SteamGrpcClients
import bruhcollective.itaysonlab.ksteam.handlers.Account
import bruhcollective.itaysonlab.ksteam.handlers.Configuration
import bruhcollective.itaysonlab.ksteam.handlers.Logger
import bruhcollective.itaysonlab.ksteam.handlers.UnifiedMessages
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.network.CMClientState
import bruhcollective.itaysonlab.ksteam.persistence.KsteamPersistenceDriver
import bruhcollective.itaysonlab.ksteam.platform.DeviceInformation
import bruhcollective.itaysonlab.ksteam.web.WebApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import okio.Path

/**
 * The actual kSteam main client which processes requests to/from Steam.
 *
 * For creating a kSteam instance, use the [kSteam] function instead, which provides more user-friendly configuration.
 */
interface SteamClient {
    /**
     * Provides simplified access to saved kSteam settings and account data.
     */
    val configuration: Configuration

    /**
     * Provides kSteam logging functions along with an ability to change verbosity or logging transport.
     */
    val logger: Logger

    /**
     * Provides REST/AJAX API accessing primitives.
     */
    val webApi: WebApi

    /**
     * Provides built-in Steam Network packet dumper.
     */
    val dumper: PacketDumper

    /**
     * Returns a [StateFlow] that describes current CM client state. Can be used to determine if authorization is required.
     */
    val connectionStatus: StateFlow<CMClientState>

    /**
     * Returns currently signed in [SteamId]. Defaults to [SteamId.Empty] if authorization was not yet completed.
     */
    val currentSessionSteamId: SteamId

    // Configuration

    /**
     * Returns the currently selected language for Steam requests.
     */
    val language: ELanguage

    /**
     * Returns current working directory where authorization and database data should be located.
     */
    val workingDirectory: Path

    /**
     * Returns initialized persistence driver to work with the stored data. [configuration] is built on top of this.
     */
    val persistence: KsteamPersistenceDriver

    /**
     * Returns initialized device information that is used for authorization tasks.
     */
    val deviceInfo: DeviceInformation

    /**
     * Returns a source of private IP used when creating a session.
     */
    val authPrivateIpLogic: AuthPrivateIpLogic

    // Subsystems

    /**
     * [Account] subsystem, used to sign in to Steam accounts.
     */
    val account: Account

    /**
     * [UnifiedMessages] subsystem, used to send and receive protobuf-encoded RPC requests over the Steam Network.
     */
    val unifiedMessages: UnifiedMessages

    /**
     * "gRPC" subsystem that provides ready to use clients that use [unifiedMessages].
     */
    val grpc: SteamGrpcClients

    // Methods

    /**
     * Main function, which you need to call before doing anything with kSteam.
     *
     * This will establish connection with Steam Network servers.
     */
    suspend fun start()

    /**
     * Stops this client. Also releases resources used by networking layer.
     *
     * You should not use this instance of [SteamClient] after calling [stop].
     */
    fun stop()

    /**
     * Subscribes to a change of [CMClientState].
     *
     * @param status what status is required to have
     * @return a [Job] to cancel the execution if needed
     */
    fun onClientState(status: CMClientState, consumer: suspend () -> Unit): Job

    /**
     * Subscribes to incoming messages of the specific type. This will only process "jobless" messages (messages that are not a response to something).
     *
     * Note that [consumer] will be called in kSteam Events scope without suspending the subscription.
     *
     * @param id a [EMsg] that needs to be received
     * @param consumer a receiver for incoming messages
     * @return a [Job] to cancel the execution if needed
     */
    fun on(id: EMsg, consumer: suspend (SteamPacket) -> Unit): Job

    /**
     * Subscribes to incoming RPC messages of the specific type. This will only process "jobless" messages (messages that are not a response to something).
     *
     * Note that [consumer] will be called in kSteam Events scope without suspending the subscription.
     *
     * @param method a RPC definition like "Service.Message"
     * @param consumer a receiver for incoming messages
     * @return a [Job] to cancel the execution if needed
     */
    fun onRpc(method: String, consumer: suspend (SteamPacket) -> Unit): Job

    /**
     * Execute a [SteamPacket] and await for a response.
     */
    suspend fun execute(packet: SteamPacket): SteamPacket

    /**
     * Execute a [SteamPacket] and subscribe for a set of responses. It is the caller responsibility to close the [Flow].
     */
    suspend fun subscribe(packet: SteamPacket): Flow<SteamPacket>

    /**
     * Execute a [SteamPacket] without waiting for a response.
     */
    suspend fun executeAndForget(packet: SteamPacket)

    /**
     * Returns if the client allows the use of CM sockets.
     */
    fun cmNetworkEnabled(): Boolean
}