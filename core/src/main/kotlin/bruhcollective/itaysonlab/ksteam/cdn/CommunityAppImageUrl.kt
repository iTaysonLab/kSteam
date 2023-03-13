package bruhcollective.itaysonlab.ksteam.cdn

import androidx.compose.runtime.Immutable
import bruhcollective.itaysonlab.ksteam.cdn.internal.CdnConstants

/**
 * A wrapper for a URL to wrap images linked to an app on SteamStatic CDN.
 */
@JvmInline
@Immutable
value class CommunityAppImageUrl(
    private val packed: Pair<Int, String>
): CdnUrl {
    override val url: String get() = "${CdnConstants.SteamStatic}/steamcommunity/public/images/apps/${packed.first}/${packed.second}.jpg"
}