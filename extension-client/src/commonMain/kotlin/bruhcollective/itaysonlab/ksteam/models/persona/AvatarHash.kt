package bruhcollective.itaysonlab.ksteam.models.persona

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import kotlin.jvm.JvmInline

@JvmInline
value class AvatarHash internal constructor(internal val hash: String) {
    companion object {
        val Empty = AvatarHash("")
    }

    val hasAvatar get() = hash.isNotEmpty()

    val small get() = "${base}.jpg"
    val medium get() = "${base}_medium.jpg"
    val full get() = "${base}_full.jpg"

    private val base get() = "${EnvironmentConstants.AVATAR_CDN_BASE}${hash}"
}