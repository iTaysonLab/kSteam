package bruhcollective.itaysonlab.ksteam.messages

import okio.Buffer

interface SteamBinaryPayloadAdapter<T> {
    fun decode(buffer: Buffer): T
    fun encode(obj: T): ByteArray
}