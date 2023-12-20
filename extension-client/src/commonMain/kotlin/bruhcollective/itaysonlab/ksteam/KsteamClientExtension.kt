package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.database.KSteamRealmDatabase
import bruhcollective.itaysonlab.ksteam.database.keyvalue.NoopKeyValueDatabase
import bruhcollective.itaysonlab.ksteam.database.keyvalue.PicsVdfKvDatabase
import bruhcollective.itaysonlab.ksteam.extension.Extension
import bruhcollective.itaysonlab.ksteam.extension.ExtensionFactory
import bruhcollective.itaysonlab.ksteam.extension.HandlerMap
import bruhcollective.itaysonlab.ksteam.extension.associate
import bruhcollective.itaysonlab.ksteam.handlers.*
import bruhcollective.itaysonlab.ksteam.handlers.guard.Guard
import bruhcollective.itaysonlab.ksteam.handlers.guard.GuardConfirmation
import bruhcollective.itaysonlab.ksteam.handlers.library.Library
import bruhcollective.itaysonlab.ksteam.handlers.library.Pics

class KsteamClient (
    private val configuration: KsteamClientExtensionConfiguration
): Extension {
    override fun createHandlers(steamClient: SteamClient): HandlerMap {
        val database = KSteamRealmDatabase(workingDirectory = steamClient.workingDirectory)

        return mapOf(
            CurrentPersona().associate(),
            News(steamClient).associate(),
            Notifications(steamClient).associate(),
            Persona(steamClient, database).associate(),
            Profile(steamClient).associate(),
            Store(steamClient).associate(),
            Player(steamClient).associate(),
            UserNews(steamClient).associate(),
            PublishedFiles(steamClient).associate(),
            // Guard
            Guard(steamClient).associate(),
            GuardConfirmation(steamClient).associate(),
            GuardManagement(steamClient).associate()
        ) + if (configuration.enablePics) {
            mapOf(
                Pics(steamClient, PicsVdfKvDatabase(NoopKeyValueDatabase)).associate(),
                Library(steamClient).associate()
            )
        } else emptyMap()
    }

    companion object Builder: ExtensionFactory<KsteamClientExtensionConfiguration, KsteamClient> {
        override fun create(configuration: KsteamClientExtensionConfiguration.() -> Unit): KsteamClient {
            return KsteamClient(KsteamClientExtensionConfiguration().apply(configuration))
        }
    }
}

class KsteamClientExtensionConfiguration {
    /**
     * Enable the PICS infrastructure. This will decrease memory usage along with faster startup process.
     *
     * What will be affected:
     * - [Store] handler will no longer use PICS metadata to speed up metadata fetching for owned apps
     * - [Pics] and [Library] handlers will no longer be accessible (a [ClientComponentDisabledException] will be thrown)
     * - [Pics] will no longer auto-fetch new licenses on start or runtime user purchases
     * - [Library] will no longer auto-fetch user's library information (last played games and collections)
     *
     * Recommended Value: true if you need to access user's library or access CDN tokens
     * Default: true
     */
    var enablePics: Boolean = true

    /**
     *
     */
}