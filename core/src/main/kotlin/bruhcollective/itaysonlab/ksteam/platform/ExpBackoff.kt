package bruhcollective.itaysonlab.ksteam.platform

import kotlinx.coroutines.delay

/**
 * An implementation for exponential backoff algorithm.
 */
internal class ExpBackoff (
    private val baseDelay: Int,
    private val maxDelay: Int = baseDelay * 5,
) {
    private var factor = 0

    suspend fun wait() {
        factor++
        delay(
            (baseDelay * factor).coerceIn(0..maxDelay).toLong()
        )
    }
}