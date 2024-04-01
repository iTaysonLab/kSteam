package bruhcollective.itaysonlab.ksteam.guard.models

import kotlin.jvm.JvmInline

@JvmInline
value class StaticAuthCode(private val packed: Pair<String, Long>) {
    val codeString: String get() = packed.first
    val generationTime: Long get() = packed.second

    operator fun component1() = codeString
    operator fun component2() = generationTime
}