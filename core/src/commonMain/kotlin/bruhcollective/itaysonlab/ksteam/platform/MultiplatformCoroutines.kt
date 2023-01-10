package bruhcollective.itaysonlab.ksteam.platform

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

expect val MultiplatformIODispatcher: CoroutineDispatcher

inline fun CreateSupervisedCoroutineScope(name: String, dispatcher: CoroutineDispatcher) = CoroutineScope(dispatcher + SupervisorJob() + CoroutineName("kSteam-$name"))

inline fun CreateSupervisedCoroutineScope(name: String, dispatcher: CoroutineDispatcher, crossinline onError: (CoroutineContext, Throwable) -> Unit) = CreateSupervisedCoroutineScope(name, dispatcher) + CoroutineExceptionHandler(onError)