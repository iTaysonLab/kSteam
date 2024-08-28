package bruhcollective.itaysonlab.ksteam.models.enums

/**
 * Equals to EPublishedFileInfoMatchingFileType described on https://partner.steamgames.com/doc/webapi/IPublishedFileService#EPublishedFileInfoMatchingFileType
 */
enum class EPublishedFileInfoMatchingFileType {
    /**
     * Items.
     */
    Items,

    /**
     * A collection of Workshop items.
     */
    Collections,

    /**
     * Artwork.
     */
    Artwork,

    /**
     * Videos.
     */
    Videos,

    /**
     * Screenshots.
     */
    Screenshots,

    /**
     * Items that can be put inside a collection.
     */
    CollectionEligible,

    /**
     * Unused.
     */
    Games,

    /**
     * Unused.
     */
    Software,

    /**
     * Unused.
     */
    Concepts,

    /**
     * Unused.
     */
    GreenlightItems,

    /**
     * Guides.
     */
    AllGuides,

    /**
     * Steam web guide.
     */
    WebGuides,

    /**
     * Application integrated guide.
     */
    IntegratedGuides,

    /**
     * No description provided.
     */
    UsableInGame,

    /**
     * Workshop merchandise meant to be voted on for the purpose of being sold.
     */
    Merch,

    /**
     * Steam Controller bindings.
     */
    ControllerBindings,

    /**
     * Used internally.
     */
    SteamworksAccessInvites,

    /**
     * Workshop items that can be sold in-game.
     */
    ItemsMtx,

    /**
     * Workshop items that can be used right away by the user.
     */
    ItemsReadyToUse,

    /**
     * No description provided.
     */
    WorkshopShowcase,

    /**
     * Managed completely by the game, not the user, and not shown on the web.
     */
    GameManagedItems
}