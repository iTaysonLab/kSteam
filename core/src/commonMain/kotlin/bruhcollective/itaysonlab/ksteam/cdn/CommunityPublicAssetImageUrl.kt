package bruhcollective.itaysonlab.ksteam.cdn

import kotlin.jvm.JvmInline

/**
 * A wrapper for a URL to wrap images linked to an app on SteamStatic CDN.
 */
@JvmInline
value class CommunityPublicAssetImageUrl(
    private val path: String
): CdnUrl {
    override val url: String get() = "https://cdn.akamai.steamstatic.com/steamcommunity/public/images/$path"
}