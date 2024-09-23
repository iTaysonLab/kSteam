package bruhcollective.itaysonlab.ksteam.network.event

import androidx.collection.mutableScatterMapOf
import bruhcollective.itaysonlab.ksteam.handlers.Logger
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.messages.SteamPacketHeader
import bruhcollective.itaysonlab.ksteam.models.enums.EMsg
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter

/**
 * An incoming event manager for CM clients.
 */
internal class IncomingPacketManager (
    val logger: Logger
) {
    // Raw listeners
    private val rawPacketListeners = mutableScatterMapOf<EMsg, MutableList<PacketListener>>()

    // Typed listeners
    private val typedPacketListeners = mutableScatterMapOf<EMsg, MutableList<TypedProtobufPacketListener<*>>>()
    private val typedPacketAdapters = mutableScatterMapOf<EMsg, ProtoAdapter<*>>()

    // Typed RPC listeners
    private val typedRpcListeners = mutableScatterMapOf<String, MutableList<TypedProtobufPacketListener<*>>>()
    private val typedRpcAdapters = mutableScatterMapOf<String, ProtoAdapter<*>>()

    // Registration
    fun registerPacketListener(msg: EMsg, listener: PacketListener) {
        rawPacketListeners.getOrPut(msg) { mutableListOf() }.add(listener)
    }

    fun unregisterPacketListener(msg: EMsg, listener: PacketListener) {
        rawPacketListeners[msg]?.remove(listener)
    }

    fun <T: Message<T, *>> registerTypedPacketListener(msg: EMsg, adapter: ProtoAdapter<T>, listener: TypedProtobufPacketListener<T>) {
        if (typedPacketAdapters.containsKey(msg).not()) {
            typedPacketAdapters[msg] = adapter
        }

        typedPacketListeners.getOrPut(msg) { mutableListOf() }.add(listener)
    }

    fun unregisterTypedPacketListener(msg: EMsg, listener: TypedProtobufPacketListener<*>) {
        typedPacketListeners[msg]?.also { listenerList ->
            listenerList.remove(listener)
        }?.size?.takeIf { it == 0 }?.let {
            // Remove the adapter if no listeners remain attached
            typedPacketAdapters.remove(msg)
        }
    }

    fun <T: Message<T, *>> registerTypedRpcListener(rpc: String, adapter: ProtoAdapter<T>, listener: TypedProtobufPacketListener<T>) {
        if (typedRpcAdapters.containsKey(rpc).not()) {
            typedRpcAdapters[rpc] = adapter
        }

        typedRpcListeners.getOrPut(rpc) { mutableListOf() }.add(listener)
    }

    fun unregisterTypedRpcListener(rpc: String, listener: TypedProtobufPacketListener<*>) {
        typedRpcListeners[rpc]?.also { listenerList ->
            listenerList.remove(listener)
        }?.size?.takeIf { it == 0 }?.let {
            // Remove the adapter if no listeners remain attached
            typedRpcAdapters.remove(rpc)
        }
    }

    // Handling
    fun handleIncomingPacket(packet: SteamPacket) {
        rawPacketListeners[packet.messageId]?.forEach { listener -> listener.onPacketReceived(packet) }

        if (packet.messageId == EMsg.k_EMsgServiceMethod && packet.header.targetJobId == 0L) {
            deliverPacketToTypedRpcProto(rpcMethod = (packet.header as SteamPacketHeader.Protobuf).targetJobName.orEmpty(), payload = packet.payload)
        } else {
            deliverPacketToTypedProto(packet)
        }
    }

    private fun deliverPacketToTypedRpcProto(rpcMethod: String, payload: ByteArray) {
        typedRpcListeners[rpcMethod]?.takeIf(List<*>::isNotEmpty)?.let { resolvedTypedPacketListeners ->
            runCatching {
                typedRpcAdapters[rpcMethod]!!.decode(payload)!!
            }.onFailure {
                logger.logWarning("IncomingPacketManager") { "[RPC] Failed to deliver packet $rpcMethod due to parsing exception: ${it.message}" }
                it.printStackTrace()
            }.onSuccess { message ->
                resolvedTypedPacketListeners.forEach { listener ->
                    runCatching {
                        listener.onPayloadObjectReceived(message)
                    }.onFailure {
                        logger.logWarning("IncomingPacketManager") { "[RPC] Failed to deliver packet $rpcMethod to listener $listener due to exception: ${it.message}" }
                        it.printStackTrace()
                    }
                }
            }
        }
    }

    private fun deliverPacketToTypedProto(packet: SteamPacket) {
        typedPacketListeners[packet.messageId]?.takeIf(List<*>::isNotEmpty)?.let { resolvedTypedPacketListeners ->
            runCatching {
                typedPacketAdapters[packet.messageId]!!.decode(packet.payload)!!
            }.onFailure {
                logger.logWarning("IncomingPacketManager") { "Failed to deliver packet ${packet.messageId} due to parsing exception: ${it.message}" }
                it.printStackTrace()
            }.onSuccess { message ->
                resolvedTypedPacketListeners.forEach { listener ->
                    runCatching {
                        listener.onPayloadObjectReceived(message)
                    }.onFailure {
                        logger.logWarning("IncomingPacketManager") { "Failed to deliver packet ${packet.messageId} to listener ${listener} due to exception: ${it.message}" }
                        it.printStackTrace()
                    }
                }
            }
        }
    }
}