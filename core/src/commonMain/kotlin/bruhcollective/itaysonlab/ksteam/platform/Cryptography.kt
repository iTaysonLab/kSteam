package bruhcollective.itaysonlab.ksteam.platform

internal expect fun encryptWithRsa(data: String, modulus: String, exponent: String): ByteArray