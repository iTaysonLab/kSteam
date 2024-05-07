package bruhcollective.itaysonlab.ksteam.debug

import android.util.Log
import bruhcollective.itaysonlab.ksteam.handlers.Logger

object AndroidLoggingTransport: Logger.Transport {
    override fun onVerbosityChanged(verbosity: Logger.Verbosity) = Unit

    override fun printVerbose(tag: String, message: () -> String) {
        Log.v(tag, message())
    }

    override fun printDebug(tag: String, message: () -> String) {
        Log.d(tag, message())
    }

    override fun printWarning(tag: String, message: () -> String) {
        Log.w(tag, message())
    }

    override fun printError(tag: String, message: () -> String) {
        Log.e(tag, message())
    }
}