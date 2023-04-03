package bruhcollective.itaysonlab.ksteam.models

import bruhcollective.itaysonlab.ksteam.models.enums.EResult
import kotlin.jvm.JvmInline

@JvmInline
value class Result<T> internal constructor(private val packed: Pair<T?, EResult>) {
    val data: T
        get() {
            return packed.first ?: error("data is null but non-null delegate was requested")
        }

    val dataNullable: T? get() = packed.first

    val result get() = packed.second

    val hasData get() = packed.first != null
    val isSuccess get() = result == EResult.OK
    val isFailure get() = result != EResult.OK
}