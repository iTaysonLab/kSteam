package bruhcollective.itaysonlab.ksteam.models

import bruhcollective.itaysonlab.ksteam.EnvironmentConstants

@JvmInline
value class AvatarHash internal constructor(private val hash: String) {
    val small get() = "${base}.jpg"
    val medium get() = "${base}_medium.jpg"
    val full get() = "${base}_full.jpg"

    private val base get() = "${EnvironmentConstants.AVATAR_CDN_BASE}/${hash}"
}