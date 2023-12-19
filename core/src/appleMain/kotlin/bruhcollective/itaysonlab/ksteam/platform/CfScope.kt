@file:OptIn(ExperimentalForeignApi::class)

package bruhcollective.itaysonlab.ksteam.platform

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain

class CfScope {
    private var typeRefs = mutableListOf<CFTypeRef>()

    fun createRef(from: Any?): CFTypeRef? {
        return CFBridgingRetain(from)?.also {
            typeRefs += it
        }
    }

    fun clear() {
        typeRefs.forEach(::CFBridgingRelease)
        typeRefs.clear()
    }
}

inline fun <R> cfScoped(block: MemScope.(CfScope) -> R): R = memScoped {
    val cfScope = CfScope()

    try {
        block(cfScope)
    } finally {
        cfScope.clear()
    }
}

fun MemScope.cfDictionaryOf(vararg pairs: Pair<CFStringRef?, CFTypeRef?>): CFDictionaryRef? {
    return cfDictionaryOf(mapOf(*pairs))
}

fun MemScope.cfDictionaryOf(map: Map<CFStringRef?, CFTypeRef?>): CFDictionaryRef? {
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