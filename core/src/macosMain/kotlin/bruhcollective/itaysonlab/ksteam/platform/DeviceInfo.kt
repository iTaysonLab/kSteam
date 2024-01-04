package bruhcollective.itaysonlab.ksteam.platform

import platform.Foundation.NSFileManager

internal actual fun getDefaultWorkingDirectory(): String? {
    return NSFileManager.defaultManager.currentDirectoryPath
}