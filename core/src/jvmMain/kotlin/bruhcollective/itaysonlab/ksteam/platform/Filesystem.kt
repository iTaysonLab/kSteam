package bruhcollective.itaysonlab.ksteam.platform

import okio.FileSystem

actual fun provideOkioFilesystem() = FileSystem.SYSTEM