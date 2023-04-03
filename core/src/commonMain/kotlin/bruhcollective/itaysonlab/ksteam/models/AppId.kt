package bruhcollective.itaysonlab.ksteam.models

import kotlin.jvm.JvmInline

@JvmInline
value class AppId(val id: Int) {
    internal val asLong get() = id.toLong()
}