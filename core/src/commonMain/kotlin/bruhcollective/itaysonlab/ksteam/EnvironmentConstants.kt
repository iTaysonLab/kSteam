package bruhcollective.itaysonlab.ksteam

object EnvironmentConstants {
    const val PROTOCOL_VERSION = 65580

    const val WEB_API_BASE = "https://api.steampowered.com/"
    const val COMMUNITY_API_BASE = "https://steamcommunity.com/"
    const val STORE_API_BASE = "https://store.steampowered.com/"
    const val AVATAR_CDN_BASE = "https://avatars.cloudflare.steamstatic.com/"

    const val CDN_BASE_CLOUDFLARE = "https://cdn.cloudflare.steamstatic.com"
    const val CDN_BASE_AKAMAI = "https://cdn.akamai.steamstatic.com"

    const val CDN_SHARED_CLOUDFLARE = "https://shared.cloudflare.steamstatic.com"
    const val CDN_SHARED_AKAMAI = "https://shared.akamai.steamstatic.com"

    // IDK about what CDN they prefer to use
    const val CDN_BASE = CDN_BASE_CLOUDFLARE
    const val CDN_SHARED = CDN_SHARED_CLOUDFLARE

    fun formatStaticAppImageUrl(appId: Int, filename: String): String
        = "$CDN_BASE/steam/apps/${appId}/${filename}"

    fun formatCommunityImageUrl(appId: Int, filename: String): String
        = formatCommunityPublicAssetUrl("apps/${appId}/${filename}")

    fun formatCommunityPublicAssetUrl(path: String): String
        = "$CDN_BASE/steamcommunity/public/images/$path"

    // https://shared.cloudflare.steamstatic.com/store_item_assets/steam/apps/292030/library_600x900_russian.jpg
    fun formatSharedStoreAssetUrl(appId: Int, filename: String): String
        = "$CDN_SHARED/store_item_assets/steam/apps/${appId}/${filename}"
}