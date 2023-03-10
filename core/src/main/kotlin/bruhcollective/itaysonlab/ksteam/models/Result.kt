package bruhcollective.itaysonlab.ksteam.models

import bruhcollective.itaysonlab.ksteam.models.enums.EResult

@JvmInline
value class Result<T> internal constructor(private val packed: Pair<T?, EResult>) {
    val data: T
        get() {
            require(packed.first != null) { "Data is null, check the result value before accessing it" }
            return packed.first!!
        }

    val dataNullable: T? get() = packed.first

    val result get() = packed.second

    val hasData get() = packed.first != null
    val isSuccess get() = result == EResult.OK
    val isFailure get() = result != EResult.OK
}