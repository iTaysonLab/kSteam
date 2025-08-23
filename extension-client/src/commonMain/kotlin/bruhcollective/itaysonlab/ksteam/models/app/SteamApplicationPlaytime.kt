package bruhcollective.itaysonlab.ksteam.models.app

import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Describes current account's playtime for a Steam application.
 */
@OptIn(ExperimentalTime::class)
data class SteamApplicationPlaytime (
    /**
     * First launch timestamp. Total here is the earliest launch timestamp.
     */
    val firstLaunch: PlatformTimestamps,

    /**
     * Last launch timestamp. Total here is the most recent launch timestamp.
     */
    val lastLaunch: PlatformTimestamps,

    /**
     * Playtime, in seconds. Total here is the playtime from all platforms.
     */
    val playTime: PlatformDurations,

    /**
     * Time unit for disconnected playtime.
     */
    val disconnectedPlaytime: Int,

    /**
     * Time unit for total playtime during last 2 weeks.
     */
    val playTimeRecent: Int
) {
    /**
     * A timestamp that is separated between platforms.
     */
    data class PlatformTimestamps (
        /**
         * Total time unit from all platforms.
         */
        val total: Instant?,

        /**
         * Time unit played on Steam Deck.
         */
        val deck: Instant?,

        /**
         * Time unit played on Windows.
         */
        val windows: Instant?,

        /**
         * Time unit played on Linux (with the Steam Deck)
         */
        val linux: Instant?,

        /**
         * Time unit played on macOS.
         */
        val mac: Instant?
    )

    /**
     * A timestamp that is separated between platforms.
     */
    data class PlatformDurations (
        /**
         * Total time unit from all platforms.
         */
        val total: Duration,

        /**
         * Time unit played on Steam Deck.
         */
        val deck: Duration,

        /**
         * Time unit played on Windows.
         */
        val windows: Duration,

        /**
         * Time unit played on Linux (with the Steam Deck)
         */
        val linux: Duration,

        /**
         * Time unit played on macOS.
         */
        val mac: Duration
    )
}