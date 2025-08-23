package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.handlers.Logger
import bruhcollective.itaysonlab.ksteam.models.enums.ELanguage
import bruhcollective.itaysonlab.ksteam.persistence.KsteamPersistenceDriver
import bruhcollective.itaysonlab.ksteam.persistence.MemoryPersistenceDriver
import bruhcollective.itaysonlab.ksteam.platform.ConnectivityStateDelayer
import bruhcollective.itaysonlab.ksteam.platform.DeviceInformation
import bruhcollective.itaysonlab.ksteam.platform.getDefaultWorkingDirectory
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import okio.Path
import okio.Path.Companion.toPath

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
internal annotation class KsteamDsl

/**
 * A starting configuration for the [SteamClient].
 *
 * To modify values, use the [kSteam] method.
 */
@KsteamDsl
class KSteamConfiguration {
    private var ktorEngineResolver: () -> HttpClient = { HttpClient(CIO) }

    /**
     * Specifies a logging transport where kSteam will log output.
     * 
     * Defaults to [Logger.Transport.Noop], which will consume logs without any actions.
     */
    var loggingTransport: Logger.Transport = Logger.Transport.Noop

    /**
     * Specifies a logging verbosity (how much data will be logged)
     * 
     * Recommended:
     * - [Logger.Verbosity.Disable] if you are using kSteam in an end-user application
     * - [Logger.Verbosity.Warning] if you are using kSteam in an end-user application and want logs
     * - [Logger.Verbosity.Debug] if you are developing an end-user application
     * - [Logger.Verbosity.Verbose] if you are developing kSteam
     * 
     * [Logger.Verbosity.Verbose] can output sensitive info to a chosen [Logger.Transport]. A warning will be printed if it is selected.
     *
     * Defaults to [Logger.Verbosity.Warning], which will cover Error and Warning messages.
     */
    var loggingVerbosity: Logger.Verbosity = Logger.Verbosity.Warning

    /**
     * Specifies a folder where kSteam will store session data.
     */
    var rootFolder: Path? = null
        set(value) {
            field = value ?: error("rootFolder must not be null")
        }

    /**
     * Device information used for the new auth flow and Steam Guard.
     */
    var deviceInfo: DeviceInformation = DeviceInformation()

    /**
     * Specifies a language used in some Steam Web API requests.
     */
    // TODO: add a way to dynamically resolve this
    var language: ELanguage = ELanguage.English

    /**
     * Specifies logic used in Login ID usage when signing in.
     *
     * Only change this if you are sure what are you doing.
     */
    var authPrivateIpLogic: AuthPrivateIpLogic = AuthPrivateIpLogic.UsePrivateIp

    /**
     * Supplies persistence implementation for kSteam handlers to use.
     *
     * You might use `core-persistence` module for ready-to-use platform implementations.
     */
    var persistenceDriver: KsteamPersistenceDriver = MemoryPersistenceDriver

    /**
     * Connectivity state delayer suspends connection until an Internet connection is established.
     *
     * This can help in avoiding requests spam when network is temporarily unavailable.
     */
    var connectivityStateDelayer: ConnectivityStateDelayer = ConnectivityStateDelayer.Noop

    /**
     * Installs a custom Ktor [HttpClient]. Defaults to using cross-platform Ktor's CIO engine.
     *
     * Useful if you need additional tweaks or a different engine that is more applicable for your task/platform.
     *
     * @param resolver lambda which returns [HttpClient] to use in kSteam
     */
    fun ktor(resolver: () -> HttpClient) {
        ktorEngineResolver = resolver
    }

    /**
     * Builds a [SteamClient] with the chosen configuration.
     *
     * After building, call [SteamClient.start] to send/receive packets.
     */
    fun build(): SteamClient {
        return SteamClientImpl(
            config = SteamClientConfiguration(
                rootFolder = rootFolder ?: getDefaultWorkingDirectory()?.toPath(normalize = true) ?: error("Current platform does not support auto-resolving of the working directory. Please, set it manually in the kSteam DSL."),
                ktorEngineResolver = ktorEngineResolver,
                deviceInfo = deviceInfo,
                language = language,
                authPrivateIpLogic = authPrivateIpLogic,
                persistenceDriver = persistenceDriver,
                connectivityStateDelayer = connectivityStateDelayer,
            )
        ).apply {
            logger.transport = loggingTransport
            logger.verbosity = loggingVerbosity
        }
    }
}

/**
 * Creates an kSteam instance with a specific configuration.
 *
 * An example of bare-bones configuration will look like this:
 * ```kotlin
 * val client = kSteam {
 *      rootFolder = [Path]
 * }
 * ```
 *
 * **NOTE:** After creating, call [SteamClient.start] to connect to the Steam network.
 */
@KsteamDsl
inline fun kSteam(crossinline configure: KSteamConfiguration.() -> Unit): SteamClient {
    return KSteamConfiguration().apply(configure).build()
}