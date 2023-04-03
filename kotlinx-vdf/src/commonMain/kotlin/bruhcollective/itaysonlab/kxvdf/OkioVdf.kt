package bruhcollective.itaysonlab.kxvdf

import bruhcollective.itaysonlab.kxvdf.internal.VdfDecoder
import bruhcollective.itaysonlab.kxvdf.internal.VdfEncoder
import bruhcollective.itaysonlab.kxvdf.internal.VdfReader
import bruhcollective.itaysonlab.kxvdf.internal.VdfWriter
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer
import okio.BufferedSink
import okio.BufferedSource

@ExperimentalSerializationApi
fun <T> Vdf.decodeFromBufferedSource(
    deserializer: DeserializationStrategy<T>,
    source: BufferedSource
): T = VdfDecoder(this, if (binaryFormat) {
    VdfReader.BinaryImpl(readFirstInt, source)
} else {
    VdfReader.TextImpl(source)
}).decodeSerializableValue(deserializer)

@ExperimentalSerializationApi
inline fun <reified T> Vdf.decodeFromBufferedSource(source: BufferedSource): T = decodeFromBufferedSource(serializersModule.serializer(), source)

@ExperimentalSerializationApi
fun <T> Vdf.encodeToBufferedSink(
    serializer: SerializationStrategy<T>,
    sink: BufferedSink,
    value: T
) {
    VdfEncoder(this, VdfWriter(sink)).encodeSerializableValue(serializer, value)
}

@ExperimentalSerializationApi
inline fun <reified T> Vdf.encodeToBufferedSink(sink: BufferedSink, value: T) = encodeToBufferedSink(serializersModule.serializer(), sink, value)