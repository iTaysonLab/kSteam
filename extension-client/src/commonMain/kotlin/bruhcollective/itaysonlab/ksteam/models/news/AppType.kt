package bruhcollective.itaysonlab.ksteam.models.news

/**
 * App types you can request from [News].
 */
enum class AppType(
    internal val apiName: String
) {
    // Show posts from these sources...
    Library("library"), // ...games in your library
    Wishlist("wishlist"), // ...games on your wishlist
    Following("following"), // ...games you follow
    Recommended("recommended"), // ...we think you would like
    Steam("steam"), // ...news about the Steam platform
    Curator("curator"), // ...curators you follow
    Featured("featured"), // ...featured posts, like top-selling games
    Recent("recent"); // ...games you purchased or played in past 6 months

    companion object {
        // Default values in SteamJS
        val Default = arrayOf(Wishlist, Following, Recommended, Steam, Curator, Library)
    }
}