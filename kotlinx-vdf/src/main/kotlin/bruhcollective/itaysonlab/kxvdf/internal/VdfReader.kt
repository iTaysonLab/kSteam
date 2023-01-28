package bruhcollective.itaysonlab.kxvdf.internal

import bruhcollective.itaysonlab.kxvdf.Vdf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import okio.BufferedSource

@OptIn(ExperimentalSerializationApi::class)
internal open class VdfReader(private val vdf: Vdf, private val decoder: VdfDecoder): AbstractDecoder() {
    companion object {
        private const val CONSUMED_UNKNOWN = -4
    }

    private var lastKnownTag: VdfDecoder.VdfTag.NodeElement? = null

    override val serializersModule: SerializersModule
        get() = vdf.serializersModule

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (decoder.exhausted()) {
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
        if (decoder.exhausted()) {
            return CompositeDecoder.DECODE_DONE
        }

        return when (val tag = decoder.nextTag()) {
            is VdfDecoder.VdfTag.EndOfFile -> {
                return CompositeDecoder.DECODE_DONE
            }

            is VdfDecoder.VdfTag.NodeStart -> {
                val indexInDescriptor = descriptor.getElementIndex(tag.name)

                return if (indexInDescriptor == CompositeDecoder.UNKNOWN_NAME) {
                    if (vdf.ignoreUnknownKeys) {
                        // consume all
                        while (true) {
                            if ((decoder.nextTag() as? VdfDecoder.VdfTag.NodeEnd)?.name == tag.name) break
                        }

                        CONSUMED_UNKNOWN
                    } else {
                        error("detected unknown node start ${tag.name} and ignoreUnknownKeys is false")
                    }
                } else {
                    indexInDescriptor
                }
            }

            is VdfDecoder.VdfTag.NodeElement -> {
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

            is VdfDecoder.VdfTag.NodeEnd -> {
                CompositeDecoder.DECODE_DONE
            }
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): AbstractDecoder {
        return when (descriptor.kind) {
            is StructureKind.CLASS -> VdfReader(vdf, decoder)
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

    private fun <T> requireTag(ifExists: VdfDecoder.VdfTag.NodeElement.() -> T): T {
        return lastKnownTag?.let(ifExists) ?: error("requireTag called but last tag is null")
    }
}

internal class VdfDecoder(private val source: BufferedSource) {
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