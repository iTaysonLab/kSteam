package bruhcollective.itaysonlab.ksteam.models

import bruhcollective.itaysonlab.ksteam.serialization.AppIdSerializer
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * AppId identifies an application present on the Steam store.
 */
@JvmInline
@Serializable(with = AppIdSerializer::class)
value class AppId(
    val value: Int
) {
    override fun toString() = value.toString()
}