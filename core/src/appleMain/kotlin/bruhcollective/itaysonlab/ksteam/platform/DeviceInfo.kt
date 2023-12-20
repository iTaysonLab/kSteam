package bruhcollective.itaysonlab.ksteam.platform

import okio.Path
import platform.Foundation.NSUUID

actual fun getRandomUuid(): String {
    return NSUUID.UUID().UUIDString
}