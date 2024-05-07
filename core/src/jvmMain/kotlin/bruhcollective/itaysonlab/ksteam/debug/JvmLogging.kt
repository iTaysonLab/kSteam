package bruhcollective.itaysonlab.ksteam.debug

import bruhcollective.itaysonlab.ksteam.handlers.Logger
import java.util.logging.Level

/**
 * A logging transport that uses [java.util.logging.Logger] as its backend.
 */
object JavaLoggingTransport : Logger.Transport {
    private val logger = java.util.logging.Logger.getLogger("kSteam")

    override fun onVerbosityChanged(verbosity: Logger.Verbosity) {
        logger.level = when (verbosity) {
            Logger.Verbosity.Disable -> Level.OFF
            Logger.Verbosity.Error -> Level.SEVERE
            Logger.Verbosity.Warning -> Level.WARNING
            Logger.Verbosity.Debug -> Level.FINE
            Logger.Verbosity.Verbose -> Level.ALL
        }
    }

    override fun printError(tag: String, message: () -> String) {
        logger.severe(createMsg(tag, message))
    }

    override fun printWarning(tag: String, message: () -> String) {
        logger.warning(createMsg(tag, message))
    }

    override fun printDebug(tag: String, message: () -> String) {
        logger.fine(createMsg(tag, message))
    }

    override fun printVerbose(tag: String, message: () -> String) {
        logger.finest(createMsg(tag, message))
    }

    private fun createMsg(tag: String, message: () -> String) = "[$tag] ${message()}"
}