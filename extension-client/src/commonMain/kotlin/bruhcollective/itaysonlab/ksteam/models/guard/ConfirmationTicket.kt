package bruhcollective.itaysonlab.ksteam.models.guard

import kotlin.jvm.JvmInline

@JvmInline
value class ConfirmationTicket(private val packed: Pair<String, Long>) {
    val b64EncodedSignature: String get() = packed.first
    val generationTime: Long get() = packed.second
}