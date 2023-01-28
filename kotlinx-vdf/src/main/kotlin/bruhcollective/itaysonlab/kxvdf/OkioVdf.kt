package bruhcollective.itaysonlab.kxvdf

import bruhcollective.itaysonlab.kxvdf.internal.VdfDecoder
import bruhcollective.itaysonlab.kxvdf.internal.VdfReader
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
import okio.BufferedSource

@ExperimentalSerializationApi
fun <T> Vdf.decodeFromBufferedSource(
    deserializer: DeserializationStrategy<T>,
    source: BufferedSource
): T = VdfDecoder(this, VdfReader(source)).decodeSerializableValue(deserializer)

@ExperimentalSerializationApi
inline fun <reified T> Vdf.decodeFromBufferedSource(source: BufferedSource): T = decodeFromBufferedSource(serializersModule.serializer(), source)