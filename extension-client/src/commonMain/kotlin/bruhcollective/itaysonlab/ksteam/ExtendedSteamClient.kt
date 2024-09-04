package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.database.KSteamRealmDatabase
import bruhcollective.itaysonlab.ksteam.handlers.*
import bruhcollective.itaysonlab.ksteam.handlers.guard.Guard
import bruhcollective.itaysonlab.ksteam.handlers.guard.GuardConfirmation
import bruhcollective.itaysonlab.ksteam.handlers.guard.GuardManagement
import bruhcollective.itaysonlab.ksteam.handlers.library.Library
import bruhcollective.itaysonlab.ksteam.handlers.library.Pics

/**
 * An object that holds [SteamClient] with an extra handlers that are helpful in building UI applications.
 */
class ExtendedSteamClient (
    val enablePics: Boolean,
    val client: SteamClient
): SteamClient by client {
    internal val database = KSteamRealmDatabase(workingDirectory = client.workingDirectory)

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
    val clientCommunication = ClientCommunication(this)
    internal val cloudConfiguration: CloudConfiguration = CloudConfiguration(this)

    // Subsystems (Guard)
    val guard: Guard = Guard(this)
    val guardConfirmation: GuardConfirmation = GuardConfirmation(this)
    val guardManagement: GuardManagement = GuardManagement(this)

    // Subsystems (PICS)
    private val picsContainer: PicsContainer? = createPicsContainer()
    val pics: Pics get() = picsContainer?.pics ?: throwPicsDisabledException()
    val library: Library get() = picsContainer?.library ?: throwPicsDisabledException()

    init {
        account.registerLogonAttemptListener { id ->
            database.initializeUserRealm(id)
        }
    }

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