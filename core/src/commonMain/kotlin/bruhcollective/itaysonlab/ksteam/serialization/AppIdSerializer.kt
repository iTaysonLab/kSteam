package bruhcollective.itaysonlab.ksteam.serialization

import bruhcollective.itaysonlab.ksteam.models.AppId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class AppIdSerializer: KSerializer<AppId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AppID", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: AppId) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): AppId {
        return AppId(decoder.decodeInt())
    }
}