package bruhcollective.itaysonlab.ksteam.models

@JvmInline
value class AppId(val id: UInt) {
    internal val asInt get() = id.toInt()
    internal val asLong get() = id.toLong()
}

fun AppId(id: Int): AppId = AppId(id.toUInt())