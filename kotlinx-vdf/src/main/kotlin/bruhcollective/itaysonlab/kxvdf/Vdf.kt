package bruhcollective.itaysonlab.kxvdf

import bruhcollective.itaysonlab.kxvdf.internal.VdfDecoder
import bruhcollective.itaysonlab.kxvdf.internal.VdfEncoder
import bruhcollective.itaysonlab.kxvdf.internal.VdfReader
import bruhcollective.itaysonlab.kxvdf.internal.VdfWriter
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import okio.Buffer

/**
 * A KotlinX Serialization module for supporting Valve Data Format files a.k.a. VDF.
 *
 * Notes:
 * - (Text) supports only "valid" VDFs (all strings are separated by " sign)
 * - support only KeyValues format (the use case is for Steam Network which apparently only uses KV format)
 */
@ExperimentalSerializationApi
sealed class Vdf (
    internal val encodeDefaults: Boolean,
    internal val ignoreUnknownKeys: Boolean,
    internal val binaryFormat: Boolean,
    internal val readFirstInt: Boolean,
    override val serializersModule: SerializersModule
): StringFormat {
    companion object Default : Vdf(false, false, false, false, EmptySerializersModule())

    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        return Buffer().also { buffer ->
            VdfEncoder(this, VdfWriter(buffer)).encodeSerializableValue(serializer, value)
        }.readString(Charsets.UTF_8)
    }

    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        return Buffer().apply { writeString(string, Charsets.UTF_8) }.let { source ->
            VdfDecoder(this, if (binaryFormat) {
                VdfReader.BinaryImpl(readFirstInt, source)
            } else {
                VdfReader.TextImpl(source)
            }).decodeSerializableValue(deserializer)
        }
    }
}

@ExperimentalSerializationApi
private class VdfImpl(
    encodeDefaults: Boolean, ignoreUnknownKeys: Boolean, binaryFormat: Boolean, readFirstInt: Boolean, serializersModule: SerializersModule
): Vdf(encodeDefaults, ignoreUnknownKeys, binaryFormat, readFirstInt, serializersModule)

@ExperimentalSerializationApi
fun Vdf(from: Vdf = Vdf, builderAction: VdfBuilder.() -> Unit): Vdf {
    return VdfBuilder(from).apply(builderAction).let { builder ->
        VdfImpl(builder.encodeDefaults, builder.ignoreUnknownKeys, builder.binaryFormat, builder.readFirstInt, builder.serializersModule)
    }
}

/**
 * Builder of the [VdfBuilder] instance provided by `Vdf` factory function.
 */
@ExperimentalSerializationApi
class VdfBuilder internal constructor(vdf: Vdf) {
    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     */
    var encodeDefaults: Boolean = vdf.encodeDefaults

    /**
     * Specifies whether encounters of unknown properties in the input VDF should be ignored instead of throwing [SerializationException].
     *
     * `false` by default.
     */
    var ignoreUnknownKeys: Boolean = vdf.ignoreUnknownKeys

    /**
     * Encodes/decodes VDF by using a "binary" format (a sequence of bytes instead of a full-blown text formatting)
     */
    var binaryFormat: Boolean = vdf.binaryFormat

    /**
     * (Binary mode only) Consume first 4 bytes (int) when reading. Some binary VDFs require this (package information, for example)
     */
    var readFirstInt: Boolean = vdf.readFirstInt

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [Vdf] instance.
     */
    var serializersModule: SerializersModule = vdf.serializersModule
}