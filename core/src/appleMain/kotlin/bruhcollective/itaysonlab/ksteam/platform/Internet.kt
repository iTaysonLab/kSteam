package bruhcollective.itaysonlab.ksteam.platform

import bruhcollective.itaysonlab.ksteam.util.generateIpV4Int

actual fun getIpv4Address(): UInt {
    // App Store's safest way to get an IP.... just generate it!
    return generateIpV4Int()
}