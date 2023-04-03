package bruhcollective.itaysonlab.ksteam.platform

import bruhcollective.itaysonlab.ksteam.platform.extensions.appleCreateSecKey
import bruhcollective.itaysonlab.ksteam.platform.extensions.appleEncryptSecData

actual fun encryptWithRsa(data: String, modulus: String, exponent: String): ByteArray {
    val publicKey = createAsn1 {
        sequence {
            hexInteger(modulus)
            hexInteger(exponent)
        }
    }

    return appleEncryptSecData(
        key = appleCreateSecKey(
            keySize = 2048,
            publicKey = publicKey
        ), utf8PlainText = data
    )
}