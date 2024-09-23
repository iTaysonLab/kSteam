package bruhcollective.itaysonlab.ksteam.network.event

fun interface TypedProtobufPacketListener <T> {
    fun onPayloadReceived(payload: T)

    @Suppress("UNCHECKED_CAST")
    fun onPayloadObjectReceived(msg: Any) { onPayloadReceived(msg as T) }
}