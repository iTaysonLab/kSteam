package bruhcollective.itaysonlab.ksteam.platform

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Security.*

actual fun encryptWithRsa(data: String, modulus: String, exponent: String) = AppleCryptography.rsaEncrypt(data, modulus, exponent)

private object AppleCryptography {
    private const val RSA = "RSA/None/PKCS1Padding"

    private fun createKey(modulus: String, exponent: String): SecKeyRef? {
        return memScoped {
            val publicKey = createAsn1 {
                sequence {
                    hexInteger(modulus)
                    hexInteger(exponent)
                }
            }

            val keySize = alloc<IntVar>().apply {
                value = 2048
            }

            val attributes = cfDictionaryOf(
                kSecAttrKeyType to kSecAttrKeyTypeRSA,
                kSecAttrKeyClass to kSecAttrKeyClassPublic,
                kSecAttrKeySizeInBits to keySize.ptr,
                kSecAttrIsPermanent to kCFBooleanFalse,
                kSecReturnPersistentRef to kCFBooleanFalse
            )

            SecKeyCreateWithData(
                keyData = publicKey.toCFData(),
                attributes = attributes,
                error = null
            )
        }
    }

    private fun MemScope.cfDictionaryOf(vararg pairs: Pair<CFStringRef?, CFTypeRef?>): CFDictionaryRef? {
        return cfDictionaryOf(mapOf(*pairs))
    }

    private fun MemScope.cfDictionaryOf(map: Map<CFStringRef?, CFTypeRef?>): CFDictionaryRef? {
        val size = map.size.toLong()

        val keys = allocArrayOf(*map.keys.toTypedArray())
        val values = allocArrayOf(*map.values.toTypedArray())

        return CFDictionaryCreate(
            kCFAllocatorDefault,
            keys.reinterpret(),
            values.reinterpret(),
            size,
            null,
            null
        )
    }

    fun rsaEncrypt(plainText: String, modulus: String, exponent: String): ByteArray {
        return memScoped {
            val key = createKey(modulus, exponent) ?: error("The key returned from SecKeyCreateWithData is null, that is not supposed to be here")

            SecKeyCreateEncryptedData(
                key = key,
                algorithm = kSecKeyAlgorithmRSAEncryptionPKCS1,
                plaintext = plainText.encodeToByteArray().toCFData(),
                error = null
            )?.toByteArray() ?: error("SecKeyCreateEncryptedData returned null, that is not supposed to be here")
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun ByteArray.toCFData(): CFDataRef =
        CFDataCreate(null,
            asUByteArray().refTo(0),
            size.toLong())!!

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun CFDataRef.toByteArray(): ByteArray {
        val length = CFDataGetLength(this)

        return UByteArray(length.toInt()).apply {
            val range = CFRangeMake(0, length)
            CFDataGetBytes(this@toByteArray, range, refTo(0))
        }.asByteArray()
    }
}