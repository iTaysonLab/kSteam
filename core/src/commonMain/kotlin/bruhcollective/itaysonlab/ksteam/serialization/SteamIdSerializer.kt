package bruhcollective.itaysonlab.ksteam.serialization

import bruhcollective.itaysonlab.ksteam.models.SteamId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class SteamIdSerializer: KSerializer<SteamId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SteamID", PrimitiveKind.LONG)

    override fun serialize(
        encoder: Encoder,
        value: SteamId
    ) {
        encoder.encodeLong(value.id.toLong())
    }

    override fun deserialize(decoder: Decoder): SteamId {
        return SteamId(decoder.decodeLong().toULong())
    }
}