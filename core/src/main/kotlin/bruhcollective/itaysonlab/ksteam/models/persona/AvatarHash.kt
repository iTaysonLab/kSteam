package bruhcollective.itaysonlab.ksteam.models.persona

import androidx.compose.runtime.Immutable
import bruhcollective.itaysonlab.ksteam.EnvironmentConstants

@JvmInline
@Immutable
value class AvatarHash internal constructor(private val hash: String) {
    val hasAvatar get() = hash.isNotEmpty()

    val small get() = "${base}.jpg"
    val medium get() = "${base}_medium.jpg"
    val full get() = "${base}_full.jpg"

    private val base get() = "${EnvironmentConstants.AVATAR_CDN_BASE}/${hash}"
}