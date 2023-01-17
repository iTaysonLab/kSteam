package bruhcollective.itaysonlab.ksteam.models.econ

@JvmInline
value class EconItemReference(private val packed: Triple<Int, Int, Long>) {
    val appId get() = packed.first
    val contextId get() = packed.second
    val assetId get() = packed.third
}