package bruhcollective.itaysonlab.ksteam.models.econ

import kotlin.jvm.JvmInline

@JvmInline
value class EconItemReference internal constructor(private val packed: Triple<Int, Int, Long>) {
    val appId get() = packed.first
    val contextId get() = packed.second
    val assetId get() = packed.third
}