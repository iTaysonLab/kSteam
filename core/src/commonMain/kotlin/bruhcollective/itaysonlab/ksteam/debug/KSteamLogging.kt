package bruhcollective.itaysonlab.ksteam.debug

object KSteamLogging {
    var transport: LoggingTransport = NoopLoggingTransport
        set(value) {
            field = value
            field.onVerbosityChanged(verbosity)
        }

    inline fun logVerbose(tag: String, crossinline message: () -> String) {
        if (verbosity.atLeast(KSteamLoggingVerbosity.Verbose)) {
            transport.printVerbose(tag, message())
        }
    }

    inline fun logDebug(tag: String, crossinline message: () -> String) {
        if (verbosity.atLeast(KSteamLoggingVerbosity.Debug)) {
            transport.printDebug(tag, message())
        }
    }

    inline fun logWarning(tag: String, crossinline message: () -> String) {
        if (verbosity.atLeast(KSteamLoggingVerbosity.Warning)) {
            transport.printWarning(tag, message())
        }
    }

    inline fun logError(tag: String, crossinline message: () -> String) {
        if (verbosity.atLeast(KSteamLoggingVerbosity.Error)) {
            transport.printError(tag, message())
        }
    }

    var verbosity: KSteamLoggingVerbosity = KSteamLoggingVerbosity.Warning
        set(value) {
            field = value
            transport.onVerbosityChanged(value)

            if (value == KSteamLoggingVerbosity.Verbose) {
                transport.printError(
                    "KSteamLogging:SetVerbosity",
                    "Verbosity level set to VERBOSE. This exposures message contents, which includes sign in information and other private data. Be careful and don't forget to redact/review data in the output."
                )
            }
        }

    val enableVerboseLogs get() = verbosity == KSteamLoggingVerbosity.Verbose
}

enum class KSteamLoggingVerbosity {
    Disable, Error, Warning, Debug, Verbose;

    // Error < Warning < Debug
    fun atLeast(other: KSteamLoggingVerbosity) = other.ordinal <= ordinal
}

interface LoggingTransport {
    fun onVerbosityChanged(verbosity: KSteamLoggingVerbosity) {}
    fun printError(tag: String, message: String)
    fun printWarning(tag: String, message: String)
    fun printDebug(tag: String, message: String)
    fun printVerbose(tag: String, message: String)
}

object NoopLoggingTransport : LoggingTransport {
    override fun printError(tag: String, message: String) = Unit
    override fun printWarning(tag: String, message: String) = Unit
    override fun printDebug(tag: String, message: String) = Unit
    override fun printVerbose(tag: String, message: String) = Unit
}

object StdoutLoggingTransport : LoggingTransport {
    override fun printError(tag: String, message: String) {
        println("[E] [$tag] $message")
    }

    override fun printWarning(tag: String, message: String) {
        println("[W] [$tag] $message")
    }

    override fun printDebug(tag: String, message: String) {
        println("[D] [$tag] $message")
    }

    override fun printVerbose(tag: String, message: String) {
        println("[V] [$tag] $message")
    }
}