package bruhcollective.itaysonlab.ksteam.extension

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.handlers.BaseHandler
import kotlin.reflect.KClass

/**
 * Describes a kSteam extension.
 */
interface Extension {
    /**
     * A list of handlers to be registered on kSteam initialization.
     *
     * This should be thought as a "finalize" function.
     */
    fun createHandlers(steamClient: SteamClient): HandlerMap

}

interface ExtensionFactory <Configuration, Extension> {
    fun create(configuration: Configuration.() -> Unit): Extension
}

inline fun <reified T: BaseHandler> T.associate() = T::class to this
typealias HandlerMap = Map<KClass<*>, BaseHandler>