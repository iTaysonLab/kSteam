@file:OptIn(BetaInteropApi::class)

package bruhcollective.itaysonlab.ksteam.platform.extensions

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class)
inline fun Boolean.toCfBoolean(): CFTypeRef? {
    return if (this) {
        kCFBooleanTrue
    } else {
        kCFBooleanFalse
    }
}

@Suppress("CAST_NEVER_SUCCEEDS")
inline fun NSData.toKotlinString(): String {
    return NSString.create(data = this, encoding = NSUTF8StringEncoding) as String
}