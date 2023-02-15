package bruhcollective.itaysonlab.ksteam.models

@JvmInline
value class AppId(val id: UInt) {
    constructor(id: Int): this(id.toUInt())

    internal val asInt get() = id.toInt()
    internal val asLong get() = id.toLong()
}