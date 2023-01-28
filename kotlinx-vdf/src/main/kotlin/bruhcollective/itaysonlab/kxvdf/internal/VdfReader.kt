package bruhcollective.itaysonlab.kxvdf.internal

import bruhcollective.itaysonlab.kxvdf.Vdf
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

    private var lastKnownTag: VdfReader.VdfTag.NodeElement? = null

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
            is VdfReader.VdfTag.NodeEnd, is VdfReader.VdfTag.EndOfFile -> {
                CompositeDecoder.DECODE_DONE
            }

            is VdfReader.VdfTag.NodeStart -> {
                val indexInDescriptor = descriptor.getElementIndex(tag.name)

                return if (indexInDescriptor == CompositeDecoder.UNKNOWN_NAME) {
                    if (vdf.ignoreUnknownKeys) {
                        // consume all
                        while (true) {
                            if ((reader.nextTag() as? VdfReader.VdfTag.NodeEnd)?.name == tag.name) break
                        }

                        CONSUMED_UNKNOWN
                    } else {
                        error("detected unknown node start ${tag.name} and ignoreUnknownKeys is false")
                    }
                } else {
                    indexInDescriptor
                }
            }

            is VdfReader.VdfTag.NodeElement -> {
                val indexInDescriptor = descriptor.getElementIndex(tag.name)

                if (indexInDescriptor == CompositeDecoder.UNKNOWN_NAME) {
                    if (vdf.ignoreUnknownKeys.not()) {
                        error("detected unknown node element ${tag.name} and ignoreUnknownKeys is false")
                    } else {
                        CONSUMED_UNKNOWN
                    }
                } else {
                    lastKnownTag = tag
                    return indexInDescriptor
                }
            }
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): AbstractDecoder {
        return when (descriptor.kind) {
            is StructureKind.CLASS -> VdfDecoder(vdf, reader)
            else -> this
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        // ?
    }

    override fun decodeBoolean() = requireTag {
        value == "1"
    }

    override fun decodeString() = requireTag {
        value
    }

    override fun decodeInt() = requireTag {
        value.toInt()
    }

    override fun decodeFloat() = requireTag {
        value.toFloat()
    }

    override fun decodeLong() = requireTag {
        value.toLong()
    }

    override fun decodeShort() = requireTag {
        value.toShort()
    }

    private fun <T> requireTag(ifExists: VdfReader.VdfTag.NodeElement.() -> T): T {
        return lastKnownTag?.let(ifExists) ?: error("requireTag called but last tag is null")
    }
}

internal class VdfReader(private val source: BufferedSource) {
    private var nodeseption = ArrayDeque<String>()

    private fun consumeWhitespace() {
        while (Character.isWhitespace(peekUtf8CodePoint())) {
            source.readUtf8CodePoint()
        }
    }

    fun nextTag(): VdfTag {
        consumeWhitespace()

        when (peekUtf8CodePoint()) {
            ControlPoints.Quote -> {
                source.readUtf8CodePoint()

                val name = readStringToControlPoint()

                return if (peekUtf8CodePoint() == ControlPoints.NewLine) {
                    // Node start
                    consumeWhitespace()
                    source.readUtf8CodePoint() // also read "{"
                    nodeseption.addLast(name)
                    VdfTag.NodeStart(name)
                } else {
                    // Key-Value
                    consumeWhitespace()
                    source.readUtf8CodePoint() // also read "

                    val value = readStringToControlPoint()
                    VdfTag.NodeElement(name, value)
                }
            }

            ControlPoints.BracketClose -> {
                source.readUtf8CodePoint() // consume
                return VdfTag.NodeEnd(nodeseption.removeLast())
            }

            0 -> {
                // EOF
                return VdfTag.EndOfFile
            }

            else -> error("Unknown control point: ${peekUtf8CodePoint()}")
        }
    }

    fun exhausted() = source.exhausted()

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

    sealed class VdfTag {
        class NodeStart (val name: String): VdfTag()
        class NodeElement (val name: String, val value: String): VdfTag()
        class NodeEnd (val name: String): VdfTag()
        object EndOfFile: VdfTag()
    }
}