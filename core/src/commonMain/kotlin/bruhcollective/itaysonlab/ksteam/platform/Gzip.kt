package bruhcollective.itaysonlab.ksteam.platform

import okio.Buffer
import okio.Source

expect fun Source.readGzippedContentAsBuffer(knownUnzippedSize: Int?): Source