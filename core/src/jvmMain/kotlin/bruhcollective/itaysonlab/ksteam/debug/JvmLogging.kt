package bruhcollective.itaysonlab.ksteam.debug

import java.util.logging.Level
import java.util.logging.Logger

object JavaLoggingTransport : LoggingTransport {
    private val logger = Logger.getLogger("kSteam")

    override fun onVerbosityChanged(verbosity: KSteamLoggingVerbosity) {
        logger.level = when (verbosity) {
            KSteamLoggingVerbosity.Disable -> Level.OFF
            KSteamLoggingVerbosity.Error -> Level.SEVERE
            KSteamLoggingVerbosity.Warning -> Level.WARNING
            KSteamLoggingVerbosity.Debug -> Level.FINE
            KSteamLoggingVerbosity.Verbose -> Level.ALL
        }
    }

    override fun printError(tag: String, message: String) {
        logger.severe(createMsg(tag, message))
    }

    override fun printWarning(tag: String, message: String) {
        logger.warning(createMsg(tag, message))
    }

    override fun printDebug(tag: String, message: String) {
        logger.fine(createMsg(tag, message))
    }

    override fun printVerbose(tag: String, message: String) {
        logger.finest(createMsg(tag, message))
    }

    private fun createMsg(tag: String, message: String) = "[$tag] $message"
}