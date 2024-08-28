package bruhcollective.itaysonlab.ksteam.models.enums

/**
 * The way that a shared file will be shared with the community.
 *
 * Equals to EWorkshopFileType described on https://partner.steamgames.com/doc/api/ISteamRemoteStorage#EWorkshopFileType
 */
enum class EWorkshopFileType {
    /**
     * Normal Workshop item that can be subscribed to.
     */
    Community,

    /**
     * Workshop item that is meant to be voted on for the purpose of selling in-game.
     */
    Microtransaction,

    /**
     * A collection of Workshop items.
     */
    Collection,

    /**
     * Artwork.
     */
    Artwork,

    /**
     * External video.
     */
    Video,

    /**
     * Screenshot.
     */
    Screenshot,

    /**
     * Unused, used to be for Greenlight game entries
     */
    Game,

    /**
     * Unused, used to be for Greenlight software entries.
     */
    Software,

    /**
     * Unused, used to be for Greenlight concepts.
     */
    Concept,

    /**
     * Steam web guide.
     */
    WebGuide,

    /**
     * Application integrated guide.
     */
    IntegratedGuide,

    /**
     * Workshop merchandise meant to be voted on for the purpose of being sold.
     */
    Merch,

    /**
     * Steam Controller bindings.
     */
    ControllerBinding,

    /**
     * Only used internally in Steam.
     */
    SteamworksAccessInvite,

    /**
     * Steam video.
     */
    SteamVideo,

    /**
     * Managed completely by the game, not the user, and not shown on the web.
     */
    GameManagedItems
}