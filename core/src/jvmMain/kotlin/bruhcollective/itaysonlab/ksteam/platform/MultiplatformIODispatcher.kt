package bruhcollective.itaysonlab.ksteam.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val MultiplatformIODispatcher: CoroutineDispatcher = Dispatchers.IO