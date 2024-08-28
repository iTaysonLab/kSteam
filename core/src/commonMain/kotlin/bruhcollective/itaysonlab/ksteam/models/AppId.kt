package bruhcollective.itaysonlab.ksteam.models

import kotlin.jvm.JvmInline

/**
 * AppId identifies an application present on the Steam store.
 */
@JvmInline
value class AppId(
    val value: Int
) {
    override fun toString() = value.toString()
}