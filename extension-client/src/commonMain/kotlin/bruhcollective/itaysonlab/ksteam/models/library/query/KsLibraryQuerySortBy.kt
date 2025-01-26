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
     * Sort by application name, honoring the "sortas" field.
     *
     * For some old games, this field contains a specially corrected name of the game for sorting:
     * - "II", "III", "IV" and other Roman numbers are replaced with "2", "3", "4"...
     * - Non-numbered games in the series will have a number (DXHR and DXMD becomes DX4 and DX5)
     * - In most of the cases, the "TM" symbol and other non-Latin symbols will be removed
     */
    NormalizedName,

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