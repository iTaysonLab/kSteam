package bruhcollective.itaysonlab.ksteam

import bruhcollective.itaysonlab.ksteam.extension.Extension
import bruhcollective.itaysonlab.ksteam.extension.ExtensionFactory
import bruhcollective.itaysonlab.ksteam.extension.HandlerMap
import bruhcollective.itaysonlab.ksteam.extension.associate
import bruhcollective.itaysonlab.ksteam.handlers.Guard
import bruhcollective.itaysonlab.ksteam.handlers.GuardConfirmation
import bruhcollective.itaysonlab.ksteam.handlers.GuardManagement

class Guard (
    private val configuration: GuardExtensionConfiguration
): Extension {
    override fun createHandlers(steamClient: SteamClient): HandlerMap = mapOf(
        Guard(steamClient).associate(),
        GuardConfirmation(steamClient).associate(),
        GuardManagement(steamClient).associate()
    )

    companion object Builder: ExtensionFactory<GuardExtensionConfiguration, bruhcollective.itaysonlab.ksteam.Guard> {
        override fun create(configuration: GuardExtensionConfiguration.() -> Unit): bruhcollective.itaysonlab.ksteam.Guard {
            return Guard(GuardExtensionConfiguration().apply(configuration))
        }
    }
}

class GuardExtensionConfiguration