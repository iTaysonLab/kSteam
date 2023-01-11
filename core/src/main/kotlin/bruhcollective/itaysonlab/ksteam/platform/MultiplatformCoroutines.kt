package bruhcollective.itaysonlab.ksteam.platform

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

inline fun CreateSupervisedCoroutineScope(name: String, dispatcher: CoroutineDispatcher) = CoroutineScope(dispatcher + SupervisorJob() + CoroutineName("kSteam-$name"))

inline fun CreateSupervisedCoroutineScope(name: String, dispatcher: CoroutineDispatcher, crossinline onError: (CoroutineContext, Throwable) -> Unit) = CreateSupervisedCoroutineScope(name, dispatcher) + CoroutineExceptionHandler(onError)