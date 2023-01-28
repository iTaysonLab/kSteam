package bruhcollective.itaysonlab.kxvdf.internal

import bruhcollective.itaysonlab.kxvdf.Vdf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule
import okio.BufferedSink

@ExperimentalSerializationApi
internal open class VdfEncoder(private val vdf: Vdf, private val writer: VdfWriter): AbstractEncoder() {
    private var hasWrittenRoot = false

    override val serializersModule: SerializersModule
        get() = vdf.serializersModule

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int) = vdf.encodeDefaults

    // This is called before actual encoding
    // Write node name + separator
    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        val childDescriptor = descriptor.getElementDescriptor(index)
        val childDescriptorName = descriptor.getElementName(index)

        when (childDescriptor.kind) {
            is StructureKind.CLASS -> {
                writer.beginNodeName(childDescriptorName)
                hasWrittenRoot = true
            }

            else -> {
                writer.writeValueName(childDescriptorName)
            }
        }

        return true
    }

    // Write node value
    override fun encodeValue(value: Any) {
        writer.writeValue(value.toString())
    }

    override fun encodeBoolean(value: Boolean) {
        writer.writeValue(if (value) "1" else "0")
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        return if (hasWrittenRoot) {
            ChildVdfEncoder(vdf, writer).also { it.beginStructure(descriptor) }
        } else {
            this
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        // writer.writeEof()
    }
}

@ExperimentalSerializationApi
private class ChildVdfEncoder(vdf: Vdf, private val writer: VdfWriter): VdfEncoder(vdf, writer) {
    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        writer.beginNode()
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        writer.endNode()
    }
}

internal class VdfWriter(private val sink: BufferedSink) {
    private var hierarchy = 0

    fun beginNodeName(nodeName: String) {
        writeHierarchy()
        sink.writeByte(ControlPoints.Quote)
        sink.writeUtf8(nodeName)
        sink.writeByte(ControlPoints.Quote)
        sink.writeByte(ControlPoints.NewLine)
    }

    fun beginNode() {
        writeHierarchy()
        hierarchy++
        sink.writeByte(ControlPoints.BracketOpen)
        sink.writeByte(ControlPoints.NewLine)
    }

    fun endNode() {
        hierarchy--
        writeHierarchy()
        sink.writeByte(ControlPoints.BracketClose)
        sink.writeByte(ControlPoints.NewLine)
    }

    fun writeValueName(name: String) {
        writeHierarchy()
        sink.writeByte(ControlPoints.Quote)
        sink.writeUtf8(name)
        sink.writeByte(ControlPoints.Quote)
        sink.writeByte(ControlPoints.HorizontalTab)
        sink.writeByte(ControlPoints.HorizontalTab)
    }

    fun writeValue(value: String) {
        sink.writeByte(ControlPoints.Quote)
        sink.writeUtf8(value)
        sink.writeByte(ControlPoints.Quote)
        sink.writeByte(ControlPoints.NewLine)
    }

    private fun writeHierarchy() {
        repeat(hierarchy) {
            sink.writeByte(ControlPoints.HorizontalTab)
        }
    }
}