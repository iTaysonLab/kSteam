package bruhcollective.itaysonlab.ksteam.models.news.usernews

import bruhcollective.itaysonlab.ksteam.models.app.SteamApplication
import bruhcollective.itaysonlab.ksteam.models.enums.EUserNewsType
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import bruhcollective.itaysonlab.ksteam.models.publishedfiles.PublishedFile
import steam.webui.usernews.CUserNews_Event
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class ActivityFeedEntry (
    val id: String,
    val date: Int
) {
    internal companion object {
        fun buildId(date: Int, persona: Persona, extra: String) = "fa_${persona.id}_$date:$extra"

        @OptIn(ExperimentalContracts::class)
        fun defaultMatch(event: CUserNews_Event, eventNext: CUserNews_Event?, type: EUserNewsType): Boolean {
            contract {
                returns(true) implies (eventNext != null)
            }

            return eventNext != null && eventNext.eventtype == type.apiEnum && eventNext.steamid_actor == event.steamid_actor
        }
    }

    /**
     * A user now owns these games from the list.
     */
    class ReceivedNewGame(
        date: Int,
        val persona: Persona,
        val apps: List<SteamApplication>,
        val packages: List<SteamApplication>,
    ): ActivityFeedEntry(id = buildId(date, persona, "rn_${apps.hashCode()}+${packages.hashCode()}"), date) {
        companion object {
            fun canMergeWith(event: CUserNews_Event, eventNext: CUserNews_Event?) = defaultMatch(event, eventNext, EUserNewsType.ReceivedNewGame)
        }

        override fun toString(): String {
            return "ReceivedNewGame(date=$date, persona=$persona, apps=${apps.joinToString()}, packages=${packages.joinToString()})"
        }
    }

    /**
     * A user posted a screenshot from a specific game.
     */
    class ScreenshotPosted(
        date: Int,
        val persona: Persona,
        val app: SteamApplication,
        val screenshot: PublishedFile.Screenshot
    ): ActivityFeedEntry(id = buildId(date, persona, "sp_${screenshot.id}"), date) {
        companion object {
            fun canMergeWith(event: CUserNews_Event, eventNext: CUserNews_Event?) = defaultMatch(event, eventNext, EUserNewsType.FilePublished_Screenshot) && event.gameid == eventNext.gameid
        }

        override fun toString(): String {
            return "ScreenshotPosted(date=$date, persona=$persona, app=${app}, screenshot=${screenshot})"
        }
    }

    /**
     * A user posted several screenshots from a specific game.
     */
    class ScreenshotsPosted(
        date: Int,
        val persona: Persona,
        val app: SteamApplication,
        val screenshots: List<PublishedFile.Screenshot>
    ): ActivityFeedEntry(id = buildId(date, persona, "sp_${screenshots.joinToString(separator = "+") { it.id.toString() }}"), date) {
        override fun toString(): String {
            return "ScreenshotsPosted(date=$date, persona=$persona, app=${app}, screenshots=[${screenshots.joinToString()}])"
        }
    }

    /**
     * A user added some games to their wishlist.
     */
    class AddedToWishlist(
        date: Int,
        val persona: Persona,
        val apps: List<SteamApplication>
    ): ActivityFeedEntry(id = buildId(date, persona, "aw_${apps.hashCode()}"), date) {
        companion object {
            fun canMergeWith(event: CUserNews_Event, eventNext: CUserNews_Event?) = defaultMatch(event, eventNext, EUserNewsType.AddedGameToWishlist)
        }

        override fun toString(): String {
            return "AddedToWishlist(date=$date, persona=$persona, apps=${apps.joinToString()})"
        }
    }

    /**
     * A user played the game for the first time.
     */
    class PlayedForFirstTime(
        date: Int,
        val persona: Persona,
        val app: SteamApplication
    ): ActivityFeedEntry(id = buildId(date, persona, "ft_${app.id}"), date) {
        override fun toString(): String {
            return "PlayedForFirstTime(date=$date, persona=$persona, app=$app)"
        }
    }

    /**
     * A user has received new achievements in this game.
     */
    class NewAchievements(
        date: Int,
        val persona: Persona,
        val app: SteamApplication,
        val achievements: List<Achievement>
    ): ActivityFeedEntry(id = buildId(date, persona, "ac_${app.id}+${achievements.hashCode()}"), date) {
        companion object {
            fun canMergeWith(event: CUserNews_Event, eventNext: CUserNews_Event?): Boolean {
                return defaultMatch(event, eventNext, EUserNewsType.AchievementUnlocked) && eventNext.gameid == event.gameid
            }
        }

        data class Achievement(
            val internalName: String,
            val displayName: String,
            val displayDescription: String,
            val icon: String,
            val unlockedPercent: Double,
            val hidden: Boolean
        )

        override fun toString(): String {
            return "NewAchievements(date=$date, persona=$persona, app=$app, achievements=[${achievements.joinToString()}])"
        }
    }

    /**
     * A user has posted a status about the game.
     */
    class PostedStatus(
        date: Int,
        val postId: Long,
        val persona: Persona,
        val app: SteamApplication,
        val text: String
    ): ActivityFeedEntry(id = buildId(date, persona, "ps_${app.id}_${postId}"), date) {
        override fun toString(): String {
            return "PostedStatus(date=$date, postId=$postId, persona=$persona, app=$app, text=$text)"
        }
    }

    /**
     * A user has added friends.
     */
    class FriendAdded(
        date: Int,
        val persona: Persona,
        val addedPersonas: List<Persona>
    ): ActivityFeedEntry(id = buildId(date, persona, "fa_${persona.id}_${addedPersonas.joinToString(separator = "+", transform = Persona::stringId)}"), date) {
        companion object {
            fun canMergeWith(event: CUserNews_Event, eventNext: CUserNews_Event?) = defaultMatch(event, eventNext, EUserNewsType.FriendAdded)
        }

        override fun toString(): String {
            return "PostedStatus(date=$date, persona=$persona, addedPersonas=${addedPersonas.joinToString()})"
        }
    }

    /**
     * kSteam does not know about this event.
     */
    class UnknownEvent(
        date: Int,
        val persona: Persona,
        val type: EUserNewsType,
        val proto: CUserNews_Event
    ): ActivityFeedEntry(id = buildId(date, persona, "unk_${persona.id}_${type.apiEnum}_${proto.hashCode()}"), date) {
        override fun toString(): String {
            return "UnknownEvent(date=$date, persona=$persona, type=$type, proto=$proto)"
        }
    }
}