package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.database.keyvalue.KeyValueDatabase
import bruhcollective.itaysonlab.ksteam.database.keyvalue.NoopKeyValueDatabase
import bruhcollective.itaysonlab.ksteam.database.keyvalue.PicsVdfKvDatabase
import bruhcollective.itaysonlab.ksteam.extension.Extension
import bruhcollective.itaysonlab.ksteam.extension.ExtensionFactory
import bruhcollective.itaysonlab.ksteam.extension.HandlerMap
import bruhcollective.itaysonlab.ksteam.extension.associate
import bruhcollective.itaysonlab.ksteam.handlers.Library
import bruhcollective.itaysonlab.ksteam.handlers.Pics
import bruhcollective.itaysonlab.ksteam.handlers.internal.CloudConfiguration

class Pics (
    configuration: PicsExtensionConfiguration
): Extension {
    private val database = PicsVdfKvDatabase(configuration.database)

    override fun createHandlers(steamClient: SteamClient): HandlerMap = mapOf(
        Pics(steamClient, database).associate(),
        Library(steamClient).associate(),
        CloudConfiguration(steamClient).associate()
    )

    companion object Builder: ExtensionFactory<PicsExtensionConfiguration, bruhcollective.itaysonlab.ksteam.Pics> {
        override fun create(configuration: PicsExtensionConfiguration.() -> Unit): bruhcollective.itaysonlab.ksteam.Pics {
            return Pics(PicsExtensionConfiguration().apply(configuration))
        }
    }
}

class PicsExtensionConfiguration {
    /**
     * Sets the [database] for [Pics] to use.
     *
     * This is required to set.
     */
    var database: KeyValueDatabase = NoopKeyValueDatabase
}