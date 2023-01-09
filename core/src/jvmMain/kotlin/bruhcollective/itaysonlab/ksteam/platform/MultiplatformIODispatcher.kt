package bruhcollective.itaysonlab.ksteam.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val MultiplatformIODispatcher: CoroutineDispatcher = Dispatchers.IO