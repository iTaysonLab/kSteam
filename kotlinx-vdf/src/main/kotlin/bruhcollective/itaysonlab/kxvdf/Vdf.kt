package bruhcollective.itaysonlab.kxvdf

import bruhcollective.itaysonlab.kxvdf.internal.VdfDecoder
import bruhcollective.itaysonlab.kxvdf.internal.VdfReader
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import okio.Buffer

/**
 * A KotlinX Serialization module for supporting Valve Data Format files a.k.a. VDF.
 *
 * Notes:
 * - supports only "valid" VDFs (all strings are separated by " sign)
 * - support only KeyValues format (the use case is for Steam Network which apparently only uses KV format)
 */
public sealed class Vdf (
    internal val encodeDefaults: Boolean,
    internal val ignoreUnknownKeys: Boolean,
    override val serializersModule: SerializersModule
): StringFormat {
    public companion object Default : Vdf(false, false, EmptySerializersModule())

    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        TODO("Not yet implemented")
    }

    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        return VdfReader(this, VdfDecoder(
            Buffer().apply { writeString(string, Charsets.UTF_8) }
        )).decodeSerializableValue(deserializer)
    }
}

private class VdfImpl(
    encodeDefaults: Boolean, ignoreUnknownKeys: Boolean, serializersModule: SerializersModule
): Vdf(encodeDefaults, ignoreUnknownKeys, serializersModule)

public fun Vdf(from: Vdf = Vdf, builderAction: VdfBuilder.() -> Unit): Vdf {
    return VdfBuilder(from).apply(builderAction).let { builder ->
        VdfImpl(builder.encodeDefaults, builder.ignoreUnknownKeys, builder.serializersModule)
    }
}


/**
 * Builder of the [VdfBuilder] instance provided by `Cbor` factory function.
 */
public class VdfBuilder internal constructor(vdf: Vdf) {

    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     */
    public var encodeDefaults: Boolean = vdf.encodeDefaults

    /**
     * Specifies whether encounters of unknown properties in the input CBOR
     * should be ignored instead of throwing [SerializationException].
     * `false` by default.
     */
    public var ignoreUnknownKeys: Boolean = vdf.ignoreUnknownKeys

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [Vdf] instance.
     */
    public var serializersModule: SerializersModule = vdf.serializersModule
}