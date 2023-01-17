package bruhcollective.itaysonlab.ksteam.debug

import java.util.logging.Level
import java.util.logging.Logger

object Logging {
    var transport: LoggingTransport = DefaultLoggingTransport()

    var verbosity: LoggingVerbosity
        get() = transport.verbosity
        set(value) {
            transport.verbosity = value
            if (value == LoggingVerbosity.Verbose) {
                transport.printError("KS::Logging", "Verbosity level set to VERBOSE. This exposures message contents, which includes sign in information and other private data. Be careful.")
            }
        }
}

enum class LoggingVerbosity {
    Disable, Error, Warning, Debug, Verbose
}

interface LoggingTransport {
    var verbosity: LoggingVerbosity

    fun printError(tag: String, message: String)
    fun printWarning(tag: String, message: String)
    fun printDebug(tag: String, message: String)
    fun printVerbose(tag: String, message: String)
}

class DefaultLoggingTransport: LoggingTransport {
    private val logger = Logger.getLogger("kSteam")

    override var verbosity: LoggingVerbosity
        get() = when (logger.level) {
            Level.SEVERE -> LoggingVerbosity.Error
            Level.WARNING -> LoggingVerbosity.Warning
            Level.FINE -> LoggingVerbosity.Debug
            Level.ALL -> LoggingVerbosity.Verbose
            else -> LoggingVerbosity.Disable
        }
        set(value) {
            logger.level = when (value) {
                LoggingVerbosity.Disable -> Level.OFF
                LoggingVerbosity.Error -> Level.SEVERE
                LoggingVerbosity.Warning -> Level.WARNING
                LoggingVerbosity.Debug -> Level.FINE
                LoggingVerbosity.Verbose -> Level.ALL
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

fun logVerbose(tag: String, message: String) = Logging.transport.printVerbose(tag, message)
fun logDebug(tag: String, message: String) = Logging.transport.printDebug(tag, message)
fun logWarning(tag: String, message: String) = Logging.transport.printWarning(tag, message)
fun logError(tag: String, message: String) = Logging.transport.printError(tag, message)