package bruhcollective.itaysonlab.ksteam.util

import steam.webui.common.CMsgIPAddress
import kotlin.random.Random

val CMsgIPAddress.ipString: String
    get() {
        return buildString {
            if (v4 != null) {
                v4?.let { v4 ->
                    append((v4 shr 24) and 255)
                    append(".")
                    append((v4 shr 16) and 255)
                    append(".")
                    append((v4 shr 8) and 255)
                    append(".")
                    append((v4 shr 0) and 255)
                }
            } else if (v6 != null) {
                append("IPv6 not supported for parsing yet")
            }
        }
    }

internal fun ipv4ToCmIpAddress(a0: Int, a1: Int, a2: Int, a3: Int): Int {
    var address = a3 and 0xFF

    address = address or ((a2 shl 8) and 0xFF00)
    address = address or ((a1 shl 16) and 0xFF0000)
    address = address or ((a0 shl 24) and 0xFF000000.toInt())

    return address
}

internal fun generateIpV4Int(): UInt {
    return ipv4ToCmIpAddress(192, 168, 0, Random.nextInt(0, 255)).toUInt()
}

internal fun obfuscateIp(src: UInt): Int {
    return (src xor 0xBAADF00D_u).toInt()
}

internal fun convertToCmIpV4(obfuscatedIpV4: UInt): CMsgIPAddress {
    return CMsgIPAddress(v4 = obfuscateIp(obfuscatedIpV4))
}