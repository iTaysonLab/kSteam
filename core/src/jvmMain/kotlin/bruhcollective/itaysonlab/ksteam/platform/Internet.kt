package bruhcollective.itaysonlab.ksteam.platform

import java.net.InetAddress
import java.nio.ByteBuffer

actual fun getIpv4Address(): UInt {
    return InetAddress.getLocalHost().address.let { ByteBuffer.wrap(it) }.int.toUInt()
}