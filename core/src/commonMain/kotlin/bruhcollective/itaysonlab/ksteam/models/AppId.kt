package bruhcollective.itaysonlab.ksteam.models

import kotlin.jvm.JvmInline

@JvmInline
value class AppId(val id: Int) {
    constructor(id: Long): this(id.toInt())

    internal val asLong get() = id.toLong()
}