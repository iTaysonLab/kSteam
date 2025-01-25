package bruhcollective.itaysonlab.ksteam.models.guard

import kotlin.jvm.JvmInline

@JvmInline
value class CodeModel(private val packed: Triple<String, Float, Long>) {
    companion object {
        val DefaultInstance = CodeModel(Triple("", 0f, 0L))
    }

    val code: String get() = packed.first

    // from 0f to 1f
    val progressRemaining: Float get() = packed.second

    val generatedAt: Long get() = packed.third
}