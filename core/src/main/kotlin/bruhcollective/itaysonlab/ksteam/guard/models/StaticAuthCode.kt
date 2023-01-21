package bruhcollective.itaysonlab.ksteam.guard.models

@JvmInline
value class StaticAuthCode(private val packed: Pair<String, Long>) {
    internal val _proto get() = packed
    val codeString: String get() = packed.first
    val generationTime: Long get() = packed.second
}