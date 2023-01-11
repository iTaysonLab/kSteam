package bruhcollective.itaysonlab.ksteam.models

import steam.extra.enums.EResult
import kotlin.jvm.JvmInline

@JvmInline
value class Result <T> (private val packed: Pair<T?, EResult>) {
    val data: T get() {
        require(packed.first != null) { "Data is null, check the result value before accessing it" }
        return packed.first!!
    }

    val result get() = packed.second

    val hasData get() = packed.first != null
    val isSuccess get() = result == EResult.OK
    val isFailure get() = result != EResult.OK
}