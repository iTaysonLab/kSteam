package bruhcollective.itaysonlab.ksteam.messages

import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import kotlinx.serialization.serializer
import steam.enums.EMsg

class ProtoPacketMessage <Proto : Message<Proto, *>> (
    messageId: EMsg,
    protobufAdapter: ProtoAdapter<Proto>,
): BasePacketMessage<Proto>(messageId) {

}