package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.SteamClientConfiguration.AuthPrivateIpLogic.UsePrivateIp
import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage
import bruhcollective.itaysonlab.ksteam.platform.DeviceInformation
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Describes configuration used in kSteam instances.
 */
class SteamClientConfiguration(
    /**
     * Proxy config used for Ktor's network clients.
     */
    private val ktorProxyConfig: ProxyConfig? = null,
    /**
     * Device information used for the new auth flow and Steam Guard.
     */
    internal val deviceInfo: DeviceInformation = DeviceInformation(),
    /**
     * Specifies a folder where kSteam will store session data.
     */
    internal val rootFolder: File,
    /**
     * Specifies a language used in some Steam Web API requests.
     */
    internal var language: ELanguage = ELanguage.English,
    /**
     * Specifies logic used in Login ID usage when signing in.
     */
    internal val authPrivateIpLogic: AuthPrivateIpLogic = UsePrivateIp,
) {
    init {
        rootFolder.mkdirs()
    }

    internal val networkClient = HttpClient(CIO) {
        engine {
            proxy = ktorProxyConfig
        }

        install(WebSockets)
    }

    internal val apiClient = HttpClient(CIO) {
        engine {
            proxy = ktorProxyConfig
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
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
         * Note that this method can cause collisions
         */
        UsePrivateIp,

        /**
         * Do not send any private IP.
         *
         * This can be used for some "privacy", but note that any sign-in from other device/application might log you out of this kSteam instance.
         */
        None
    }
}