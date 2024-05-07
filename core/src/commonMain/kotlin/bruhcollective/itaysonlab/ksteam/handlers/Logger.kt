package bruhcollective.itaysonlab.ksteam.handlers

/**
 * Lightweight kSteam logging infrastructure that does not depend on other libraries.
 *
 * You can write a custom [Transport] that redirects logs directly to your logging mechanism of choice.
 */
class Logger {
    var verbosity: Verbosity = Verbosity.Warning
        set(value) {
            field = value
            transport.onVerbosityChanged(value)

            if (value == Verbosity.Verbose) {
                transport.printError("KSteamLogging:SetVerbosity") {
                    """
                        Verbosity level set to VERBOSE. This exposures message contents, which includes sign in information and other private data. 
                        
                        Be careful and don't forget to redact/review data in the output.
                    """.trimIndent()
                }
            }
        }

    var transport: Transport = Transport.Noop
        set(value) {
            field = value
            field.onVerbosityChanged(verbosity)
        }

    fun logVerbose(tag: String, message: () -> String) {
        if (verbosity.atLeast(Verbosity.Verbose)) {
            transport.printVerbose(tag, message)
        }
    }

    fun logDebug(tag: String, message: () -> String) {
        if (verbosity.atLeast(Verbosity.Debug)) {
            transport.printDebug(tag, message)
        }
    }

    fun logWarning(tag: String, message: () -> String) {
        if (verbosity.atLeast(Verbosity.Warning)) {
            transport.printWarning(tag, message)
        }
    }

    fun logError(tag: String, message: () -> String) {
        if (verbosity.atLeast(Verbosity.Error)) {
            transport.printError(tag, message)
        }
    }

    enum class Verbosity {
        Disable, Error, Warning, Debug, Verbose;

        // Error < Warning < Debug
        fun atLeast(other: Verbosity) = other.ordinal <= ordinal
    }

    interface Transport {
        object Noop : Transport {
            override fun onVerbosityChanged(verbosity: Verbosity) = Unit
            override fun printVerbose(tag: String, message: () -> String) = Unit
            override fun printDebug(tag: String, message: () -> String) = Unit
            override fun printWarning(tag: String, message: () -> String) = Unit
            override fun printError(tag: String, message: () -> String) = Unit
        }

        object Stdout : Transport {
            override fun onVerbosityChanged(verbosity: Verbosity) = Unit

            override fun printVerbose(tag: String, message: () -> String) {
                println("[V] [$tag] ${message()}")
            }

            override fun printDebug(tag: String, message: () -> String) {
                println("[D] [$tag] ${message()}")
            }

            override fun printWarning(tag: String, message: () -> String) {
                println("[W] [$tag] ${message()}")
            }

            override fun printError(tag: String, message: () -> String) {
                println("[E] [$tag] ${message()}")
            }
        }

        fun onVerbosityChanged(verbosity: Verbosity)
        fun printVerbose(tag: String, message: () -> String)
        fun printDebug(tag: String, message: () -> String)
        fun printWarning(tag: String, message: () -> String)
        fun printError(tag: String, message: () -> String)
    }

    val enableVerboseLogs get() = verbosity == Verbosity.Verbose
}