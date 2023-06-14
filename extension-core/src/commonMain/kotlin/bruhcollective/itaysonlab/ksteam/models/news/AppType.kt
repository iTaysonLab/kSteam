package bruhcollective.itaysonlab.ksteam.models.news

/**
 * App types you can request from [News].
 */
enum class AppType(
    internal val apiName: String
) {
    Library("library"),
    Wishlist("wishlist"),
    Following("following"),
    Recommended("recommended"),
    Steam("steam"),
    Curator("curator"),
    Featured("featured")
}