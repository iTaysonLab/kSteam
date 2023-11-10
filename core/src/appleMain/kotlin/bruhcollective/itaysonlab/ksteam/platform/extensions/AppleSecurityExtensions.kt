package bruhcollective.itaysonlab.ksteam.platform.extensions

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Security.*

// publicKey is ASN.1
@OptIn(ExperimentalForeignApi::class)
internal fun appleCreateSecKey(keySize: Int, publicKey: ByteArray): SecKeyRef = memScoped {
    val keySizeAllocation = alloc<IntVar>().apply {
        value = keySize
    }

    val attributes = cfDictionaryOf(
        kSecAttrKeyType to kSecAttrKeyTypeRSA,
        kSecAttrKeyClass to kSecAttrKeyClassPublic,
        kSecAttrKeySizeInBits to keySizeAllocation.ptr,
        kSecAttrIsPermanent to kCFBooleanFalse,
        kSecReturnPersistentRef to kCFBooleanFalse
    )

    return SecKeyCreateWithData(
        keyData = publicKey.toCFData(),
        attributes = attributes,
        error = null
    ) ?: error("The key returned from SecKeyCreateWithData is null, that is not supposed to be here")
}

@OptIn(ExperimentalForeignApi::class)
internal fun appleEncryptSecData(key: SecKeyRef, utf8PlainText: String): ByteArray = memScoped {
    SecKeyCreateEncryptedData(
        key = key,
        algorithm = kSecKeyAlgorithmRSAEncryptionPKCS1,
        plaintext = utf8PlainText.encodeToByteArray().toCFData(),
        error = null
    )?.toByteArray() ?: error("SecKeyCreateEncryptedData returned null, that is not supposed to be here")
}

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalForeignApi::class)
internal fun ByteArray.toCFData(): CFDataRef =
    CFDataCreate(null,
        asUByteArray().refTo(0),
        size.toLong())!!

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalForeignApi::class)
internal fun CFDataRef.toByteArray(): ByteArray {
    val length = CFDataGetLength(this)

    return UByteArray(length.toInt()).apply {
        val range = CFRangeMake(0, length)
        CFDataGetBytes(this@toByteArray, range, refTo(0))
    }.asByteArray()
}

@OptIn(ExperimentalForeignApi::class)
private fun MemScope.cfDictionaryOf(vararg pairs: Pair<CFStringRef?, CFTypeRef?>): CFDictionaryRef? {
    return cfDictionaryOf(mapOf(*pairs))
}

@OptIn(ExperimentalForeignApi::class)
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