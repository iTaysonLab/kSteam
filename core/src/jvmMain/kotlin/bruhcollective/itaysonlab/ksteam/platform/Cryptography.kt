package bruhcollective.itaysonlab.ksteam.platform

import java.math.BigInteger
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher

actual fun encryptWithRsa(data: String, modulus: String, exponent: String) = JvmCryptography.rsaEncrypt(data, modulus, exponent)

private object JvmCryptography {
    private const val RSA = "RSA/None/PKCS1Padding"

    private fun createKey(modulus: String, exponent: String): RSAPublicKeySpec {
        return RSAPublicKeySpec(BigInteger(modulus, 16), BigInteger(exponent, 16))
    }

    private fun rsaEncrypt(data: String, key: RSAPublicKeySpec): ByteArray {
        return Cipher.getInstance(RSA).apply {
            init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA").generatePublic(key))
        }.doFinal(data.encodeToByteArray())
    }

    fun rsaEncrypt(data: String, modulus: String, exponent: String) = rsaEncrypt(data, createKey(modulus, exponent))
}