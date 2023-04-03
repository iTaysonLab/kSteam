package bruhcollective.itaysonlab.ksteam.platform

import okio.Source
import okio.buffer
import okio.gzip

actual fun Source.readGzippedContentAsBuffer(knownUnzippedSize: Int?): Source = gzip().buffer()