package bruhcollective.itaysonlab.ksteam.platform

import java.util.*

actual fun getRandomUuid(): String {
    return UUID.randomUUID().toString()
}

actual fun getDefaultWorkingDirectory(): String? {
    return null
}