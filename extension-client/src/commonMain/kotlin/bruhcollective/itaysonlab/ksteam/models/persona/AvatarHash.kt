package bruhcollective.itaysonlab.ksteam.models.persona

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants
import kotlin.jvm.JvmInline

@JvmInline
value class AvatarHash internal constructor(internal val hash: String) {
    companion object {
        val Empty = AvatarHash("")
    }

    /**
     * Returns if the avatar exists.
     */
    val hasAvatar: Boolean
        get() = hash.isNotEmpty()

    /**
     * Returns the small avatar image.
     */
    val small: String
        get() = "${base}.jpg"

    /**
     * Returns the medium avatar image.
     */
    val medium: String
        get() = "${base}_medium.jpg"

    /**
     * Returns the large avatar image.
     */
    val full: String
        get() = "${base}_full.jpg"

    private val base get() = "${EnvironmentConstants.AVATAR_CDN_BASE}${hash}"
}