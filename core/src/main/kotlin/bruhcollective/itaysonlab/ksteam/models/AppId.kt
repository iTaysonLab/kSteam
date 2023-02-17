package bruhcollective.itaysonlab.ksteam.models

@JvmInline
value class AppId(val id: Int) {
    internal val asLong get() = id.toLong()
}