package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.SteamClientConfiguration.AuthPrivateIpLogic.UsePrivateIp
import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage
import bruhcollective.itaysonlab.ksteam.persistence.KsteamPersistenceDriver
import bruhcollective.itaysonlab.ksteam.persistence.MemoryPersistenceDriver
import bruhcollective.itaysonlab.ksteam.platform.DeviceInformation
import bruhcollective.itaysonlab.ksteam.platform.provideOkioFilesystem
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okio.Path

/**
 * Describes configuration used in kSteam instances.
 */
class SteamClientConfiguration(
    /**
     * Specifies default Ktor HttpClient. Defaults to cross-platform CIO.
     */
    ktorEngineResolver: () -> HttpClient = { HttpClient(CIO) },
    /**
     * Device information used for the new auth flow and Steam Guard.
     */
    internal val deviceInfo: DeviceInformation = DeviceInformation(),
    /**
     * Specifies a folder where kSteam will store session data.
     */
    internal val rootFolder: Path,
    /**
     * Specifies a language used in some Steam Web API requests.
     */
    internal var language: ELanguage = ELanguage.English,
    /**
     * Specifies logic used in Login ID usage when signing in.
     */
    internal val authPrivateIpLogic: AuthPrivateIpLogic = UsePrivateIp,
    /**
     * Persistence driver is used for saving key/value pairs of important information
     */
    internal val persistenceDriver: KsteamPersistenceDriver = MemoryPersistenceDriver
) {
    init {
        provideOkioFilesystem().createDirectories(rootFolder, mustCreate = false)
    }

    internal val networkClient = ktorEngineResolver().config {
        install(WebSockets)
    }

    internal val apiClient = ktorEngineResolver().config {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }

        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            exponentialDelay()
        }
    }

    /**
     * Specifies a source of private IP used when creating a session.
     *
     * It's recommended to use the default value of [UsePrivateIp] to avoid being logged out if another client will try to sign in.
     */
    enum class AuthPrivateIpLogic {
        /**
         * Uses current machine's private IP.
         *
         * This is the recommended approach which is used in the official client and other Steam Network libraries.
         * Note that this method can cause collisions - mostly in the situations when an official Steam client is launched on a PC with a running kSteam instance (or vice versa).
         *
         * On Apple platforms, it will fall back to [Generate] because current API usage can lead to the App Store rules violation.
         */
        UsePrivateIp,

        /**
         * Generates a random integer and passes this as an IP.
         *
         * Not recommended because there is no guarantee Steam would accept a session with "faked" IP.
         */
        Generate,

        /**
         * Do not send any private IP.
         *
         * This can be used for some "privacy", but note that any sign-in from other device/application might log you out of this kSteam instance.
         */
        None
    }
}