package bruhcollective.itaysonlab.ksteam.cdn

import bruhcollective.itaysonlab.ksteam.cdn.internal.CdnConstants
import kotlin.jvm.JvmInline

/**
 * A wrapper for a URL to wrap images linked to an app on SteamStatic CDN.
 */
@JvmInline
value class StaticAppImageUrl(
    private val packed: Pair<Int, String>
): CdnUrl {
    override val url: String get() = "${CdnConstants.SteamStatic}/steam/apps/${packed.first}/${packed.second}"
}