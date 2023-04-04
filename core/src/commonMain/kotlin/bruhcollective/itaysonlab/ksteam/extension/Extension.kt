package bruhcollective.itaysonlab.ksteam.extension

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.handlers.BaseHandler
import kotlin.reflect.KClass

/**
 * Describes a kSteam extension.
 *
 * Extension is a resolvable-at-initialization class which supplies kSteam with [BaseHandler]'s.
 */
interface Extension {
    /**
     * A list of handlers to be registered on kSteam initialization.
     *
     * This should be thought as a "finalize" function.
     */
    fun createHandlers(steamClient: SteamClient): HandlerMap
}

/**
 * Describes a kSteam extension factory.
 *
 * Your extension's companion object should implement this interface in order to be available through [bruhcollective.itaysonlab.ksteam.KSteamConfiguration.install].
 */
interface ExtensionFactory <Configuration, Extension> {
    /**
     * A "build" function which takes your "Configuration" and returns an Extension.
     */
    fun create(configuration: Configuration.() -> Unit): Extension
}

inline fun <reified T: BaseHandler> T.associate() = T::class to this
typealias HandlerMap = Map<KClass<*>, BaseHandler>