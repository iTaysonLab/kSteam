package bruhcollective.itaysonlab.ksteam.models.app

/**
 * Describes current account's playtime for a Steam application.
 */
data class SteamApplicationPlaytime (
    /**
     * First launch timestamp. Total here is the earliest launch timestamp.
     */
    val firstLaunch: PlatformTimes,

    /**
     * Last launch timestamp. Total here is the most recent launch timestamp.
     */
    val lastLaunch: PlatformTimes,

    /**
     * Playtime, in seconds. Total here is the playtime from all platforms.
     */
    val playTime: PlatformTimes
) {
    /**
     * A time unit (timestamp or seconds) that is separated between platforms.
     */
    data class PlatformTimes (
        /**
         * Total time unit from all platforms. For timestamps - the recent or the earliest one. For seconds - the total of them.
         */
        val total: Int,

        /**
         * Time unit played on Steam Deck.
         */
        val deck: Int,

        /**
         * Time unit played on Windows.
         */
        val windows: Int,

        /**
         * Time unit played on Linux (excluding Steam Deck?)
         */
        val linux: Int,

        /**
         * Time unit played on macOS.
         */
        val mac: Int
    )
}