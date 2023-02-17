package bruhcollective.itaysonlab.ksteam.cdn

import bruhcollective.itaysonlab.ksteam.cdn.internal.CdnConstants

/**
 * A wrapper for a URL to wrap images linked to an app on SteamStatic CDN.
 */
@JvmInline
value class CommunityAppImageUrl(
    private val packed: Pair<Int, String>
) {
    val url: String get() = "${CdnConstants.SteamStatic}/steamcommunity/public/images/apps/${packed.first}/${packed.second}.jpg"
}