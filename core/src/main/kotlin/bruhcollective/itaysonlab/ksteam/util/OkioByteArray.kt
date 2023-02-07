package bruhcollective.itaysonlab.ksteam.util

import okio.Buffer

internal fun ByteArray.buffer() = Buffer().write(this)