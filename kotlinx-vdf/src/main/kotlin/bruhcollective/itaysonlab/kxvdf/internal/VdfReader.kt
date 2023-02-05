package bruhcollective.itaysonlab.kxvdf.internal

import bruhcollective.itaysonlab.kxvdf.SkipperRootNode
import bruhcollective.itaysonlab.kxvdf.Vdf
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import okio.BufferedSource

@ExperimentalSerializationApi
internal open class VdfDecoder(private val vdf: Vdf, private val reader: VdfReader): AbstractDecoder() {
    companion object {
        private const val CONSUMED_UNKNOWN = -4
    }

    open fun acquireNodeIndex(descriptor: SerialDescriptor, name: String) = descriptor.getElementIndex(name)

    override val serializersModule: SerializersModule
        get() = vdf.serializersModule

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (reader.exhausted()) {
            return CompositeDecoder.DECODE_DONE
        } else {
            while (true) {
                decodeElementIndexInternal(descriptor).takeIf {
                    it != CONSUMED_UNKNOWN
                }?.let {
                    return it
                }
            }
        }
    }

    private fun decodeElementIndexInternal(descriptor: SerialDescriptor): Int {
        if (reader.exhausted()) {
            return CompositeDecoder.DECODE_DONE
        }

        return when (val tag = reader.nextTag()) {
            is VdfTag.NodeEnd, is VdfTag.EndOfFile -> {
                CompositeDecoder.DECODE_DONE
            }

            is VdfTag.NodeStart -> {
                val indexInDescriptor = descriptor.getElementIndex(tag.name)
                return if (indexInDescriptor == CompositeDecoder.UNKNOWN_NAME) {
                    if (vdf.ignoreUnknownKeys) {
                        // consume all
                        while (true) {
                            if ((reader.nextTag() as? VdfTag.NodeEnd)?.name == tag.name) break
                        }

                        CONSUMED_UNKNOWN
                    } else {
                        error("detected unknown node start ${tag.name} and ignoreUnknownKeys is false")
                    }
                } else {
                    indexInDescriptor
                }
            }

            is VdfTag.NodeElementName -> {
                val indexInDescriptor = acquireNodeIndex(descriptor, tag.name)

                if (indexInDescriptor == CompositeDecoder.UNKNOWN_NAME) {
                    if (vdf.ignoreUnknownKeys.not()) {
                        error("detected unknown node element ${tag.name} and ignoreUnknownKeys is false")
                    } else {
                        CONSUMED_UNKNOWN
                    }
                } else {
                    return indexInDescriptor
                }
            }

            is VdfTag.NodeElementValue -> {
                // Not used
                CONSUMED_UNKNOWN
            }
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): AbstractDecoder {
        return when (descriptor.kind) {
            is StructureKind.CLASS -> VdfDecoder(vdf, reader)
            is StructureKind.MAP -> VdfMapDecoder(vdf, reader)
            else -> this
        }
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T {
        if (descriptor.serialName == SkipperRootNode) {
            reader.nextTag()
        }

        return super.decodeSerializableElement(descriptor, index, deserializer, previousValue)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        // ?
    }

    override fun decodeBoolean() = requireTag { this.toBooleanStrictOrNull() ?: (this == "1") }
    override fun decodeString() = requireTag { this }
    override fun decodeInt() = requireTag(String::toInt)
    override fun decodeFloat() = requireTag(String::toFloat)
    override fun decodeLong() = requireTag(String::toLong)
    override fun decodeShort() = requireTag(String::toShort)

    open fun <T> requireTag(ifExists: String.() -> T): T {
        return reader.readValue().let(ifExists) ?: error("requireTag called but last tag is not NodeElementValue")
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal class VdfMapDecoder(vdf: Vdf, private val reader: VdfReader): VdfDecoder(vdf, reader) {
    private var indexInMap = 0
    private var values = mutableListOf<String>()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (reader.exhausted()) {
            CompositeDecoder.DECODE_DONE
        } else {
            decodeElementIndexInternal(descriptor)
        }
    }

    private fun decodeElementIndexInternal(descriptor: SerialDescriptor): Int {
        if (reader.exhausted()) {
            return CompositeDecoder.DECODE_DONE
        }

        return when (val tag = reader.nextTag()) {
            is VdfTag.NodeEnd, is VdfTag.EndOfFile -> {
                CompositeDecoder.DECODE_DONE
            }

            is VdfTag.NodeStart -> {
                values.add("")
                acquireNodeIndex(descriptor, tag.name)
            }

            is VdfTag.NodeElementName -> {
                values.add(tag.name)
                acquireNodeIndex(descriptor, tag.name)
            }

            is VdfTag.NodeElementValue -> {
                values.add(tag.value)
                acquireNodeIndex(descriptor, "")
            }
        }
    }

    override fun <T> requireTag(ifExists: String.() -> T): T {
        return values.last().let(ifExists)
    }

    override fun acquireNodeIndex(descriptor: SerialDescriptor, name: String): Int {
        return indexInMap++
    }
}

internal sealed class VdfTag {
    class NodeStart (val name: String): VdfTag()
    class NodeElementName (val name: String): VdfTag()
    class NodeElementValue (val value: String): VdfTag()
    class NodeEnd (val name: String): VdfTag()
    object EndOfFile: VdfTag()
}

internal sealed class VdfReader (internal val source: BufferedSource) {
    abstract fun nextTag(): VdfTag
    abstract fun readValue(): String
    internal fun exhausted(): Boolean = source.exhausted()

    class TextImpl(source: BufferedSource): VdfReader(source) {
        private var inNodePair = false
        private var hierarchy = ArrayDeque<String>()

        private fun consumeWhitespace() {
            while (Character.isWhitespace(peekUtf8CodePoint())) {
                source.readUtf8CodePoint()
            }
        }

        override fun readValue(): String {
            return (nextTag() as VdfTag.NodeElementValue).value
        }

        override fun nextTag(): VdfTag {
            consumeWhitespace()

            if (inNodePair) {
                inNodePair = false
                return VdfTag.NodeElementValue(readStringToControlPoint())
            }

            when (peekUtf8CodePoint()) {
                ControlPoints.Quote -> {
                    source.readUtf8CodePoint()

                    val name = readStringToControlPoint()

                    return if (peekUtf8CodePoint() == ControlPoints.NewLine) {
                        // Node start
                        consumeWhitespace()
                        source.readUtf8CodePoint() // also read "{"
                        hierarchy.addLast(name)
                        VdfTag.NodeStart(name)
                    } else {
                        // Key-Value
                        inNodePair = true
                        consumeWhitespace()
                        source.readUtf8CodePoint() // also read "
                        VdfTag.NodeElementName(name)
                    }
                }

                ControlPoints.BracketClose -> {
                    source.readUtf8CodePoint() // consume
                    return VdfTag.NodeEnd(hierarchy.removeLast())
                }

                0 -> {
                    // EOF
                    return VdfTag.EndOfFile
                }

                else -> error("Unknown control point: ${peekUtf8CodePoint()}")
            }
        }

        private fun peekUtf8CodePoint() = source.peek().readUtf8CodePoint()

        /**
         * This reads all bytes, adding them to [String], up to control point (")
         * Supports non-BMP chars.
         */
        private fun readStringToControlPoint(): String {
            return StringBuilder().apply {
                while (peekUtf8CodePoint() != ControlPoints.Quote) {
                    appendCodePoint(source.readUtf8CodePoint())
                }
            }.toString().also {
                // Also consume the "
                source.readUtf8CodePoint()
            }
        }
    }

    class BinaryImpl(readFirstInt: Boolean, source: BufferedSource): VdfReader(source) {
        private var hierarchy = ArrayDeque<String>()
        private var currentType: BinaryVdfType = BinaryVdfType.None

        init {
            if (readFirstInt) {
                source.readInt()
            }
        }

        override fun nextTag(): VdfTag {
            if (currentType != BinaryVdfType.None) {
                return VdfTag.NodeElementValue(readValue()).also {
                    currentType = BinaryVdfType.None
                }
            }

            return when (val type = BinaryVdfType.values()[source.readByte().toInt()]) {
                BinaryVdfType.End -> {
                    VdfTag.NodeEnd(hierarchy.removeLastOrNull() ?: "")
                }

                BinaryVdfType.None -> {
                    readUntilNull().let { name ->
                        hierarchy.addLast(name)
                        VdfTag.NodeStart(name)
                    }
                }

                else -> {
                    readUntilNull().let { name ->
                        currentType = type
                        VdfTag.NodeElementName(name)
                    }
                }
            }
        }

        private fun readUntilNull(): String {
            return StringBuilder().apply {
                while (true) {
                    val byte = source.readUtf8CodePoint()

                    if (byte == 0) {
                        if (isNotEmpty()) {
                            break
                        } else {
                            continue
                        }
                    }

                    appendCodePoint(byte)
                }
            }.toString()
        }

        override fun readValue(): String {
            return when (currentType) {
                BinaryVdfType.String -> readUntilNull()

                BinaryVdfType.Int32, BinaryVdfType.Float32, BinaryVdfType.Pointer, BinaryVdfType.Color -> source.readIntLe().toString()
                BinaryVdfType.Int64, BinaryVdfType.UInt64 -> source.readLongLe().toString()

                else -> {
                    error("Unsupported type for readValue: $currentType")
                }
            }.also {
                currentType = BinaryVdfType.None
            }
        }

        enum class BinaryVdfType {
            None,
            String,
            Int32,
            Float32,
            Pointer,
            Widestring,
            Color,
            UInt64,
            End,
            Int64
        }
    }
}