package bruhcollective.itaysonlab.ksteam.models.library.query

/**
 * Describes kSteam query sorting.
 */
enum class KsLibraryQuerySortBy {
    /**
     * Don't sort by anything.
     */
    None,

    /**
     * Sort by application ID.
     */
    AppId,

    /**
     * Sort by application name.
     */
    Name,

    /**
     * Sort by played time.
     *
     * **NOTE:** This will be run AFTER the initial query, which means AFTER limit operations!
     */
    PlayedTime,

    /**
     * Sort by last played time.
     *
     * **NOTE:** This will be run AFTER the initial query, which means AFTER limit operations!
     */
    LastPlayed,

    /**
     * Sort by release date.
     */
    ReleaseDate,

    /**
     * Sort by Metacritic score. Note the not all games have a Metacritic page attached to Steam.
     */
    MetacriticScore,

    /**
     * Sort by Steam review score.
     */
    SteamScore,
}