package bruhcollective.itaysonlab.ksteam.debug

object KSteamLogging {
    var transport: LoggingTransport = NoopLoggingTransport

    fun logVerbose(tag: String, message: String) = transport.printVerbose(tag, message)
    fun logDebug(tag: String, message: String) = transport.printDebug(tag, message)
    fun logWarning(tag: String, message: String) = transport.printWarning(tag, message)
    fun logError(tag: String, message: String) = transport.printError(tag, message)

    var verbosity: KSteamLoggingVerbosity
        get() = transport.verbosity
        set(value) {
            transport.verbosity = value
            if (value == KSteamLoggingVerbosity.Verbose) {
                transport.printError(
                    "Global:Logging",
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
    var verbosity: KSteamLoggingVerbosity

    fun printError(tag: String, message: String)
    fun printWarning(tag: String, message: String)
    fun printDebug(tag: String, message: String)
    fun printVerbose(tag: String, message: String)
}

object NoopLoggingTransport : LoggingTransport {
    override var verbosity: KSteamLoggingVerbosity
        get() = KSteamLoggingVerbosity.Disable
        set(value) {}

    override fun printError(tag: String, message: String) = Unit
    override fun printWarning(tag: String, message: String) = Unit
    override fun printDebug(tag: String, message: String) = Unit
    override fun printVerbose(tag: String, message: String) = Unit
}

object StdoutLoggingTransport : LoggingTransport {
    override var verbosity: KSteamLoggingVerbosity = KSteamLoggingVerbosity.Verbose
    override fun printError(tag: String, message: String) {
        if (verbosity.atLeast(KSteamLoggingVerbosity.Error)) {
            println("[E] [$tag] $message")
        }
    }

    override fun printWarning(tag: String, message: String) {
        if (verbosity.atLeast(KSteamLoggingVerbosity.Warning)) {
            println("[W] [$tag] $message")
        }
    }

    override fun printDebug(tag: String, message: String) {
        if (verbosity.atLeast(KSteamLoggingVerbosity.Debug)) {
            println("[D] [$tag] $message")
        }
    }

    override fun printVerbose(tag: String, message: String) {
        if (verbosity.atLeast(KSteamLoggingVerbosity.Verbose)) {
            println("[V] [$tag] $message")
        }
    }
}