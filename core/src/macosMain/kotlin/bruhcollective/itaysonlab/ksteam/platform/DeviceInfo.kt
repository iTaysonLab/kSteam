package bruhcollective.itaysonlab.ksteam.platform

import platform.Foundation.NSFileManager

actual fun getDefaultWorkingDirectory(): String? {
    return NSFileManager.defaultManager.currentDirectoryPath
}