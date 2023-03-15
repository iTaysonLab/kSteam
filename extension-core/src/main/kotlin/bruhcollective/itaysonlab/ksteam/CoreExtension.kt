package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.extension.Extension
import bruhcollective.itaysonlab.ksteam.extension.ExtensionFactory
import bruhcollective.itaysonlab.ksteam.extension.HandlerMap
import bruhcollective.itaysonlab.ksteam.extension.associate
import bruhcollective.itaysonlab.ksteam.handlers.*

class Core (
    private val configuration: CoreExtensionConfiguration
): Extension {
    override fun createHandlers(steamClient: SteamClient): HandlerMap = mapOf(
        CurrentPersona().associate(),
        News(steamClient).associate(),
        Notifications(steamClient).associate(),
        Persona(steamClient).associate(),
        Profile(steamClient).associate(),
        Store(steamClient).associate()
    )

    companion object Builder: ExtensionFactory<CoreExtensionConfiguration, Core> {
        override fun create(configuration: CoreExtensionConfiguration.() -> Unit): Core {
            return Core(CoreExtensionConfiguration().apply(configuration))
        }
    }
}

class CoreExtensionConfiguration