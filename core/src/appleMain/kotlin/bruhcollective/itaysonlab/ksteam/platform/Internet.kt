package bruhcollective.itaysonlab.ksteam.platform

import kotlin.random.Random
import kotlin.random.nextUInt

actual fun getIpv4Address(): UInt {
    // AppStore safe way to generate a integer.... just generate it!
    return Random.nextUInt()
}