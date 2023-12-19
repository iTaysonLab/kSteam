package bruhcollective.itaysonlab.ksteam.platform

import bruhcollective.itaysonlab.ksteam.platform.extensions.toCfBoolean
import bruhcollective.itaysonlab.ksteam.platform.extensions.toKotlinString
import kotlinx.cinterop.*
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Security.*
import platform.darwin.OSStatus

@OptIn(ExperimentalForeignApi::class)
typealias MutableAppleDictionary = MutableMap<CFStringRef?, CFTypeRef?>

@OptIn(ExperimentalForeignApi::class)
object Keychain {
    data class Options (
        val returnData: Boolean = false,
        val data: String? = null,
        val limitToOne: Boolean = false,
    )

    sealed interface Query {
        fun composeAppleQuery(scope: MemScope, options: Options, block: MemScope.(CFDictionaryRef?) -> OSStatus): OSStatus

        data class GenericPassword(
            val accountName: String,
            val serviceName: String? = null,
            val description: String? = null,
            val allowIcloudSynchronization: Boolean = true
        ): Query {
            override fun composeAppleQuery(scope: MemScope, options: Options, block: MemScope.(CFDictionaryRef?) -> OSStatus): OSStatus = with (scope) {
                val map: MutableAppleDictionary = mutableMapOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrSynchronizable to allowIcloudSynchronization.toCfBoolean(),
                    kSecReturnData to options.returnData.toCfBoolean()
                )

                if (options.returnData && options.limitToOne) {
                    map[kSecMatchLimit] = kSecMatchLimitOne
                }

                val accountNameRef = accountName.let(::CFBridgingRetain)?.also { ref ->
                    map[kSecAttrAccount] = ref
                }

                val serviceNameRef = serviceName?.let(::CFBridgingRetain)?.also { ref ->
                    map[kSecAttrService] = ref
                }

                val descriptionRef = description?.let(::CFBridgingRetain)?.also { ref ->
                    map[kSecAttrDescription] = ref
                }

                val dataRef = options.data?.let(::CFBridgingRetain)?.also { ref ->
                    map[kSecValueData] = ref
                }

                val dictionaryRef = cfDictionaryOf(map)

                return try {
                    block(dictionaryRef)
                } finally {
                    accountNameRef?.let(::CFBridgingRelease)
                    serviceNameRef?.let(::CFBridgingRelease)
                    descriptionRef?.let(::CFBridgingRelease)
                    dataRef?.let(::CFBridgingRelease)
                    dictionaryRef?.let(::CFBridgingRelease)
                }
            }
        }
    }

    /**
     * Finds an item in the Keychain with the specific query.
     *
     * Returns null if the item was not found.
     */
    @OptIn(ExperimentalForeignApi::class)
    @Throws(IllegalStateException::class)
    fun findItem(
        query: Query
    ): String? = memScoped {
        val resultVar = alloc<CFTypeRefVar>()

        val resultCode = query.composeAppleQuery(
            scope = this,
            options = Options(returnData = true, limitToOne = true, data = null)
        ) { dict ->
            SecItemCopyMatching(query = dict, result = resultVar.ptr)
        }

        return@memScoped when (resultCode) {
            errSecSuccess -> (CFBridgingRelease(resultVar.value) as? NSData)?.toKotlinString()
            errSecItemNotFound -> null
            else -> resultCode.fail()
        }
    }

    /**
     * Adds an item in the Keychain with the specific query.
     *
     * @param query A [Query] that defines a Keychain item.
     * @param data A string representation of data that will be stored in Keychain.
     *
     * @throws IllegalStateException when a Keychain error occurs
     */
    @Throws(IllegalStateException::class)
    fun addItem(
        query: Query,
        data: String
    ) = memScoped {
        query.composeAppleQuery(
            scope = this,
            options = Options(returnData = false, data = data)
        ) { dict ->
            SecItemAdd(dict, null)
        }.failOnError()
    }

    /**
     * Updates an item in the Keychain with the specific query.
     *
     * @param query A [Query] that defines a Keychain item.
     * @param data A string representation of data that will be stored in Keychain.
     *
     * @throws IllegalStateException when a Keychain error occurs
     */
    @Throws(IllegalStateException::class)
    fun updateItem(
        query: Query,
        data: String
    ) = memScoped {
        val dataRef = CFBridgingRetain(data)
        val newAttributesRef = cfDictionaryOf(kSecValueData to dataRef)

        try {
            query.composeAppleQuery(
                scope = this,
                options = Options(returnData = false)
            ) { dict ->
                SecItemUpdate(dict, newAttributesRef)
            }.failOnError()
        } finally {
            CFBridgingRelease(newAttributesRef)
            CFBridgingRelease(dataRef)
        }
    }

    /**
     * Adds an item in the Keychain with the specific query. If an item already exists, updates it.
     *
     * @param query A [Query] that defines a Keychain item.
     * @param data A string representation of data that will be stored in Keychain.
     *
     * @throws IllegalStateException when a Keychain error occurs
     */
    @Throws(IllegalStateException::class)
    fun upsertItem(
        query: Query,
        data: String
    ) {
        if (findItem(query) != null) {
            updateItem(query, data)
        } else {
            addItem(query, data)
        }
    }

    /**
     * Delete an item from the Keychain with the specific query.
     *
     * @param query A [Query] that defines a Keychain item.
     *
     * @throws IllegalStateException when a Keychain error occurs
     */
    @Throws(IllegalStateException::class)
    fun deleteItem(
        query: Query
    ) = memScoped {
        query.composeAppleQuery(
            scope = this,
            options = Options(returnData = false)
        ) { dict ->
            SecItemDelete(dict)
        }.failOnError()
    }

    @Throws(IllegalStateException::class)
    private inline fun OSStatus.failOnError() {
        if (this != errSecSuccess) {
            fail()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    @Suppress("CAST_NEVER_SUCCEEDS")
    @Throws(IllegalStateException::class)
    private inline fun OSStatus.fail(): Nothing {
        val nsString = CFBridgingRelease(SecCopyErrorMessageString(this, null)) as? NSString
        val message = (nsString as? String) ?: "Unknown Keychain error: $this"
        error(message)
    }
}