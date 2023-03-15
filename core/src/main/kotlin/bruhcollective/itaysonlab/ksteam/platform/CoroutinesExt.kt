package bruhcollective.itaysonlab.ksteam.platform

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

// TODO: judge based on CPU cores (probably)
private const val DEFAULT_PARALLELISM = 8

@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun <In, Out> dispatchListProcessing(data: List<In>, parallelism: Int = DEFAULT_PARALLELISM, transformer: suspend (In) -> Out?): List<Out> = coroutineScope {
    val parseDispatcher = Dispatchers.IO.limitedParallelism(parallelism)

    data.map {
        async(parseDispatcher) {
            transformer(it)
        }
    }.awaitAll().filterNotNull()
}

@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun <In, Out> dispatchMapProcessing(data: List<In>, parallelism: Int = DEFAULT_PARALLELISM, transformer: suspend (In) -> Out?): List<Out> = coroutineScope {
    val parseDispatcher = Dispatchers.IO.limitedParallelism(parallelism)

    data.map {
        async(parseDispatcher) {
            transformer(it)
        }
    }.awaitAll().filterNotNull()
}

inline fun CreateSupervisedCoroutineScope(name: String, dispatcher: CoroutineDispatcher) =
    CoroutineScope(dispatcher + SupervisorJob() + CoroutineName("kSteam-$name"))

inline fun CreateSupervisedCoroutineScope(
    name: String,
    dispatcher: CoroutineDispatcher,
    crossinline onError: (CoroutineContext, Throwable) -> Unit
) = CreateSupervisedCoroutineScope(name, dispatcher) + CoroutineExceptionHandler(onError)