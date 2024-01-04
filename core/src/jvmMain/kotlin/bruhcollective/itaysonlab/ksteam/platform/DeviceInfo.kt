package bruhcollective.itaysonlab.ksteam.platform

import java.util.*

internal actual fun getRandomUuid(): String {
    return UUID.randomUUID().toString()
}

internal actual fun getDefaultWorkingDirectory(): String? {
    return System.getProperty("user.dir")
}