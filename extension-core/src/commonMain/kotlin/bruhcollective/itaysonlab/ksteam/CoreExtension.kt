package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.extension.Extension
import bruhcollective.itaysonlab.ksteam.extension.ExtensionFactory
import bruhcollective.itaysonlab.ksteam.extension.HandlerMap
import bruhcollective.itaysonlab.ksteam.extension.associate
import bruhcollective.itaysonlab.ksteam.handlers.CurrentPersona
import bruhcollective.itaysonlab.ksteam.handlers.News
import bruhcollective.itaysonlab.ksteam.handlers.Notifications
import bruhcollective.itaysonlab.ksteam.handlers.Persona
import bruhcollective.itaysonlab.ksteam.handlers.Player
import bruhcollective.itaysonlab.ksteam.handlers.Profile
import bruhcollective.itaysonlab.ksteam.handlers.PublishedFiles
import bruhcollective.itaysonlab.ksteam.handlers.Store
import bruhcollective.itaysonlab.ksteam.handlers.UserNews

class Core (
    private val configuration: CoreExtensionConfiguration
): Extension {
    override fun createHandlers(steamClient: SteamClient): HandlerMap = mapOf(
        CurrentPersona().associate(),
        News(steamClient).associate(),
        Notifications(steamClient).associate(),
        Persona(steamClient).associate(),
        Profile(steamClient).associate(),
        Store(steamClient).associate(),
        Player(steamClient).associate(),
        UserNews(steamClient).associate(),
        PublishedFiles(steamClient).associate(),
    )

    companion object Builder: ExtensionFactory<CoreExtensionConfiguration, Core> {
        override fun create(configuration: CoreExtensionConfiguration.() -> Unit): Core {
            return Core(CoreExtensionConfiguration().apply(configuration))
        }
    }
}

class CoreExtensionConfiguration