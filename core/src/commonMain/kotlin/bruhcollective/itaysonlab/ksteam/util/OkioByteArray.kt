package bruhcollective.itaysonlab.ksteam.util

import okio.Buffer

fun ByteArray.buffer() = Buffer().write(this)