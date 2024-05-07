package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.database.KSteamRealmDatabase
import bruhcollective.itaysonlab.ksteam.grpc.ExtendedSteamGrpcClients
import bruhcollective.itaysonlab.ksteam.grpc.ExtendedSteamGrpcClientsImpl
import bruhcollective.itaysonlab.ksteam.handlers.*
import bruhcollective.itaysonlab.ksteam.handlers.guard.Guard
import bruhcollective.itaysonlab.ksteam.handlers.guard.GuardConfirmation
import bruhcollective.itaysonlab.ksteam.handlers.guard.GuardManagement
import bruhcollective.itaysonlab.ksteam.handlers.library.Library
import bruhcollective.itaysonlab.ksteam.handlers.library.Pics
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import bruhcollective.itaysonlab.ksteam.network.CMClientState
import bruhcollective.itaysonlab.ksteam.web.WebApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * An object that holds [SteamClient] with an extra handlers that are helpful in building UI applications.
 */
class ExtendedSteamClient (
    val enablePics: Boolean,
    val client: SteamClient
) {
    private val database = KSteamRealmDatabase(workingDirectory = client.workingDirectory)

    val grpc: ExtendedSteamGrpcClients = ExtendedSteamGrpcClientsImpl(client)

    // Subsystems
    val currentPersona: CurrentPersona = CurrentPersona(this)
    val news: News = News(this)
    val notifications: Notifications = Notifications(this)
    val persona: Persona = Persona(this, database)
    val player: Player = Player(this)
    val profile: Profile = Profile(this)
    val publishedFiles: PublishedFiles = PublishedFiles(this)
    val store: Store = Store(this)
    val userNews: UserNews = UserNews(this)
    internal val cloudConfiguration: CloudConfiguration = CloudConfiguration(this)

    // Subsystems (Guard)
    val guard: Guard = Guard(this)
    val guardConfirmation: GuardConfirmation = GuardConfirmation(this)
    val guardManagement: GuardManagement = GuardManagement(this)

    // Subsystems (PICS)
    private val picsContainer: PicsContainer? = createPicsContainer()
    val pics: Pics get() = picsContainer?.pics ?: throwPicsDisabledException()
    val library: Library get() = picsContainer?.library ?: throwPicsDisabledException()

    // SteamClient subsystems
    val account: Account get() = client.account
    val configuration: Configuration get() = client.configuration
    val logger: Logger get() = client.logger
    val unifiedMessages: UnifiedMessages get() = client.unifiedMessages
    val webApi: WebApi get() = client.webApi

    // SteamClient variables pass-through
    val language: ELanguage get() = client.language
    val connectionStatus: StateFlow<CMClientState> get() = client.connectionStatus
    val currentSessionSteamId: SteamId get() = client.currentSessionSteamId

    // SteamClient API pass-through
    suspend fun start() = client.start()
    fun stop() = client.stop()
    fun on(id: EMsg, consumer: suspend (SteamPacket) -> Unit): Job = client.on(id, consumer)
    fun onRpc(method: String, consumer: suspend (SteamPacket) -> Unit): Job = client.onRpc(method, consumer)
    suspend fun execute(packet: SteamPacket): SteamPacket = client.execute(packet)
    suspend fun subscribe(packet: SteamPacket): Flow<SteamPacket> = client.subscribe(packet)
    suspend fun executeAndForget(packet: SteamPacket) = client.executeAndForget(packet)

    //

    class PicsContainer (
        val pics: Pics,
        val library: Library
    )

    private fun createPicsContainer(): PicsContainer? {
        return if (enablePics) {
            PicsContainer(
                pics = Pics(this, database),
                library = Library(this)
            )
        } else {
            null
        }
    }

    private fun throwPicsDisabledException(): Nothing {
        throw Exception("PICS was disabled: however, a PICS-required subsystem was invoked.")
    }
}

/**
 * Extends [SteamClient] with subsystems adapted for UI client development.
 *
 * @param enablePics enables PICS-related subsystems (Library and Pics), which can make startup process longer
 */
fun SteamClient.extendToClient(
    enablePics: Boolean
): ExtendedSteamClient {
    return ExtendedSteamClient(
        enablePics = enablePics,
        client = this
    )
}