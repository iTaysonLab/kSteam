package bruhcollective.itaysonlab.ksteam.platform

import platform.Foundation.NSUUID

internal actual fun getRandomUuid(): String {
    return NSUUID.UUID().UUIDString
}