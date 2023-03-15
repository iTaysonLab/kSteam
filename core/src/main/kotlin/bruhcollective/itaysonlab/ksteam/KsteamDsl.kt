package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.extension.Extension
import bruhcollective.itaysonlab.ksteam.extension.ExtensionFactory
import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage
import bruhcollective.itaysonlab.ksteam.platform.DeviceInformation
import io.ktor.client.engine.*
import java.io.File

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class KsteamDsl

@KsteamDsl
class KSteamConfiguration {
    private val extensions = mutableListOf<Extension>()

    /**
     * Specifies a folder where kSteam will store session data.
     */
    var rootFolder: File? = null
        set(value) {
            field = value ?: error("rootFolder must not be null")
        }

    /**
     * Proxy config used for Ktor's network clients.
     */
    var ktorProxyConfig: ProxyConfig? = null
        set(value) {
            field = value ?: error("ktorProxyConfig must not be null")
        }

    /**
     * Device information used for the new auth flow and Steam Guard.
     */
    var deviceInfo: DeviceInformation = DeviceInformation()

    /**
     * Specifies a language used in some Steam Web API requests.
     *
     * TODO: add a way to dynamically resolve this
     */
    var language: ELanguage = ELanguage.English

    /**
     * Specifies logic used in Login ID usage when signing in.
     *
     * Only change this if you are sure what are you doing.
     */
    var authPrivateIpLogic: SteamClientConfiguration.AuthPrivateIpLogic =
        SteamClientConfiguration.AuthPrivateIpLogic.UsePrivateIp

    fun <Configuration, Extension: bruhcollective.itaysonlab.ksteam.extension.Extension> install(
        factory: ExtensionFactory<Configuration, Extension>,
        configure: Configuration.() -> Unit = {}
    ) {
        extensions += factory.create(configure)
    }

    fun build(): SteamClient {
        return SteamClient(
            config = SteamClientConfiguration(
                rootFolder = rootFolder ?: error("rootFolder must be set"),
                ktorProxyConfig = ktorProxyConfig,
                deviceInfo = deviceInfo,
                language = language,
                authPrivateIpLogic = authPrivateIpLogic
            ), injectedExtensions = extensions
        )
    }
}

inline fun kSteam(crossinline configure: KSteamConfiguration.() -> Unit): SteamClient {
    return KSteamConfiguration().apply(configure).build()
}