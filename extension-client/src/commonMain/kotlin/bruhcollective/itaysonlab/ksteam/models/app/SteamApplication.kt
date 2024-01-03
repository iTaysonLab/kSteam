package bruhcollective.itaysonlab.ksteam.models.app

import bruhcollective.itaysonlab.ksteam.models.pics.AppInfo

/**
 * Defines an application available on Steam. Returns most of the foreground information, except for descriptions/screenshots/assets.
 *
 * This is a "layer" between PICS, Store and the end application. kSteam will try to provide as much data from offline source, but in some cases it can fall back to calling API methods.
 *
 * Because of Steam's API nature, this class will be partially complete based on:
 * - user's availability in library: if the game is purchased, much more information will be available
 * - calling handler: data requests from [Store] handler will contain both data from PICS and Store, while [Library]/[PICS] handlers will return only local data
 *
 * Steam not only hosts games, but also applications, DLC's, music albums and videos. kSteam tries to support most of special properties, however:
 * - music/video information is currently available only for purchased content via PICS results
 */
data class SteamApplication (
    /**
     * Steam application ID. Will be always positive.
     */
    val id: Int,

    /**
     * Type of the application.
     */
    val type: Type,

    /**
     * Application name.
     */
    val name: String,
) {
    enum class Type {
        /**
         * Game. Most popular content and is mostly supported by kSteam.
         */
        Game,

        /**
         * Tool. Mostly dedicated servers, Linux compatibility layers.
         */
        Tool,

        /**
         * DLC. An addition to existing [Game].
         */
        DLC,

        /**
         * Music. An addition to existing [Game], but unlike [DLC], contains music tracks (and does not require purchasing of linked [Game]).
         *
         * Special metadata is available only for purchased content.
         */
        Music,

        /**
         * Video. An addition to existing [Game], but unlike [DLC], contains a video file (and does not require purchasing of linked [Game]).
         *
         * Special metadata is available only for purchased content.
         */
        Video,

        /**
         * Configuration. Special rule sets that are not available for public view on the Steam store.
         */
        Config,

        /**
         * Demo. Linked to [Game], provides a specially crafted demo version of a game or application.
         */
        Demo,

        /**
         * Application. Ordinary applications published on Steam.
         */
        Application,

        /**
         * Beta. Linked to not released [Game], this serves as an early-access link.
         */
        Beta,

        /**
         * Unknown. Be careful - this might be an unknown object, or just an error when parsing strings.
         */
        Unknown
    }

    //

    companion object {
        /**
         * Converts a PICS [AppInfo] into a client-facing [SteamApplication].
         */
        internal fun fromPics(pics: AppInfo): SteamApplication {
            val common = pics.common ?: error("[${pics.appId}] PICS common field should not be null!")

            return SteamApplication(
                id = pics.appId,
                type = when (common.type.lowercase()) {
                    "game" -> Type.Game
                    "tool" -> Type.Tool
                    "dlc" -> Type.DLC
                    "muAsic" -> Type.Music
                    "video" -> Type.Video
                    "config" -> Type.Config
                    "application" -> Type.Application
                    "beta" -> Type.Beta
                    "demo" -> Type.Demo
                    else -> Type.Unknown
                },
                name = common.name
            )
        }
    }
}