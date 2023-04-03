package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.debug.KSteamLogging
import bruhcollective.itaysonlab.ksteam.debug.KSteamLoggingVerbosity
import bruhcollective.itaysonlab.ksteam.debug.LoggingTransport
import bruhcollective.itaysonlab.ksteam.debug.NoopLoggingTransport
import bruhcollective.itaysonlab.ksteam.extension.Extension
import bruhcollective.itaysonlab.ksteam.extension.ExtensionFactory
import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage
import bruhcollective.itaysonlab.ksteam.platform.DeviceInformation
import io.ktor.client.engine.*
import okio.Path

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class KsteamDsl

@KsteamDsl
class KSteamConfiguration {
    private val extensions = mutableListOf<Extension>()

    /**
     * Specifies a logging transport where kSteam will log output
     * 
     * Defaults to [NoopLoggingTransport], which will consume logs without any actions.
     */
    var loggingTransport: LoggingTransport = NoopLoggingTransport

    /**
     * Specifies a logging verbosity (how much data will be logged)
     * 
     * Recommended:
     * - [KSteamLoggingVerbosity.Disable] if you are using kSteam in a end-user application
     * - [KSteamLoggingVerbosity.Warning] if you are using kSteam in a end-user application and want logs
     * - [KSteamLoggingVerbosity.Debug] if you are developing an end-user application
     * - [KSteamLoggingVerbosity.Verbose] if you are developing kSteam
     * 
     * [KSteamLoggingVerbosity.Verbose] can output sensitive info to a chosen [LoggingTransport]. A warning will be printed if it is selected.
     *
     * Defaults to [KSteamLoggingVerbosity.Warning], which will cover Error and Warning messages
     */
    var loggingVerbosity: KSteamLoggingVerbosity = KSteamLoggingVerbosity.Warning

    /**
     * Specifies a folder where kSteam will store session data.
     */
    var rootFolder: Path? = null
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
        KSteamLogging.transport = loggingTransport
        KSteamLogging.verbosity = loggingVerbosity
        
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