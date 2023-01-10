package bruhcollective.itaysonlab.ksteam.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.newFixedThreadPoolContext

// Used the https://github.com/Kotlin/kotlinx.coroutines/pull/3576/ approach: we want IO-optimized thread pool on JVM, and a replacement on Native
actual val MultiplatformIODispatcher: CoroutineDispatcher = newFixedThreadPoolContext(64, "MultiplatformIODispatcher")