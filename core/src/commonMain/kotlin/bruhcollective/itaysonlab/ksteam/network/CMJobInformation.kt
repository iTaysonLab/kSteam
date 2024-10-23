package bruhcollective.itaysonlab.ksteam.network

import bruhcollective.itaysonlab.ksteam.models.enums.EMsg

// Shared information for all exceptions
data class CMJobInformation (
    val id: CMJobId,
    val name: String,
    val msgId: EMsg
) {
    override fun toString(): String {
        return "[id = $id, job = ${name.ifEmpty { msgId.name }}]"
    }
}