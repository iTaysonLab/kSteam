package bruhcollective.itaysonlab.ksteam.cdn

object SteamCdn {
    private const val SteamStatic = "https://cdn.cloudflare.steamstatic.com"

    fun formatStaticAppImageUrl(
        appId: Int, filename: String
    ) = "${SteamStatic}/steam/apps/${appId}/${filename}"

    fun formatCommunityImageUrl(
        appId: Int, filename: String
    ) = "${SteamStatic}/steamcommunity/public/images/apps/${appId}/${filename}"

    fun formatCommunityPublicAssetUrl(
        path: String
    ) = "https://cdn.akamai.steamstatic.com/steamcommunity/public/images/$path"
}