package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.SteamClientConfiguration.AuthPrivateIpLogic.UsePrivateIp
import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage
import bruhcollective.itaysonlab.ksteam.persistence.KsteamPersistenceDriver
import bruhcollective.itaysonlab.ksteam.persistence.MemoryPersistenceDriver
import bruhcollective.itaysonlab.ksteam.platform.DeviceInformation
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.SYSTEM

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
     * Persistence driver is used for saving key/value pairs of important information.
     */
    internal val persistenceDriver: KsteamPersistenceDriver = MemoryPersistenceDriver,
    /**
     * Transportation method used to transmit Steam Network packets.
     */
    internal val transportMode: TransportMode = TransportMode.WebSocket,
    /**
     * Coroutine dispatcher used for events or network operations
     */
    internal val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    init {
        FileSystem.SYSTEM.createDirectories(rootFolder, mustCreate = false)
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

    /**
     * Specifies transportation method used for receiving and sending packets through Steam infrastructure.
     */
    enum class TransportMode {
        /**
         * Allows kSteam to make a direct connection to the Steam Network by using WebSockets. CMClient will be enabled and will send/receive any binary and protobuf packets.
         *
         * This is the preferred way of using kSteam and is used by default.
         */
        WebSocket,

        /**
         * Forces kSteam to only use HTTP for transmitting information.
         * CMClient will be disabled and any request involving it will throw an [UnsupportedTransportException]. RPC requests will work.
         *
         * This is helpful for mobile devices, because it does not need an active connection all the time.
         *
         * However, a subset of API methods are not available with this mode, such as:
         * - presence of VAC bans
         * - PICS
         * - real-time notifications (chats/notifications/user state changes)
         * - Workshop content by using PublishedFile API
         * - and possibly more
         *
         * This transportation method does not require [SteamClient.start] to work, and [SteamClient.stop] is a no-op.
         */
        Web
    }
}