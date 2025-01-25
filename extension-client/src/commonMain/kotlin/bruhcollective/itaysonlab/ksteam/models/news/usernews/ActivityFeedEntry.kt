package bruhcollective.itaysonlab.ksteam.models.news.usernews

import bruhcollective.itaysonlab.ksteam.models.apps.AppSummary
import bruhcollective.itaysonlab.ksteam.models.enums.EUserNewsType
import bruhcollective.itaysonlab.ksteam.models.persona.SummaryPersona
import bruhcollective.itaysonlab.ksteam.models.publishedfiles.PublishedFile
import steam.webui.usernews.CUserNews_Event
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class ActivityFeedEntry (
    val id: String,
    val date: Int
) {
    internal companion object {
        fun buildId(date: Int, persona: SummaryPersona, extra: String) = "fa_${persona.id}_$date:$extra"

        @OptIn(ExperimentalContracts::class)
        fun defaultMatch(event: CUserNews_Event, eventNext: CUserNews_Event?, type: EUserNewsType): Boolean {
            contract {
                returns(true) implies (eventNext != null)
            }

            return eventNext != null && eventNext.eventtime == event.eventtime && eventNext.eventtype == type.apiEnum && eventNext.steamid_actor == event.steamid_actor
        }
    }

    /**
     * A user now owns these games from the list.
     */
    class ReceivedNewGame(
        date: Int,
        val persona: SummaryPersona,
        val apps: List<AppSummary>,
        val packages: List<AppSummary>,
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
        val persona: SummaryPersona,
        val app: AppSummary,
        val screenshot: PublishedFile.Screenshot
    ): ActivityFeedEntry(id = buildId(date, persona, "sp_${screenshot.id}"), date) {
        companion object {
            fun canMergeWith(event: CUserNews_Event, eventNext: CUserNews_Event?) = defaultMatch(event, eventNext, EUserNewsType.ReceivedNewGame) && event.gameid == eventNext.gameid
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
        val persona: SummaryPersona,
        val app: AppSummary,
        val screenshots: List<PublishedFile.Screenshot>
    ): ActivityFeedEntry(id = buildId(date, persona, "sp_${screenshots.joinToString(separator = "+") { it.id.toString() }}"), date) {
        override fun toString(): String {
            return "ScreenshotPosted(date=$date, persona=$persona, app=${app}, screenshots=[${screenshots.joinToString()}])"
        }
    }

    /**
     * A user added some games to their wishlist.
     */
    class AddedToWishlist(
        date: Int,
        val persona: SummaryPersona,
        val apps: List<AppSummary>
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
        val persona: SummaryPersona,
        val app: AppSummary
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
        val persona: SummaryPersona,
        val app: AppSummary,
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
        val persona: SummaryPersona,
        val app: AppSummary,
        val text: String
    ): ActivityFeedEntry(id = buildId(date, persona, "ps_${app.id}_${postId}"), date) {
        override fun toString(): String {
            return "PostedStatus(date=$date, postId=$postId, persona=$persona, app=$app, text=$text)"
        }
    }

    /**
     * kSteam does not know about this event.
     */
    class UnknownEvent(
        date: Int,
        val persona: SummaryPersona,
        val type: EUserNewsType,
        val proto: CUserNews_Event
    ): ActivityFeedEntry(id = buildId(date, persona, "unk_${persona.id}_${type.apiEnum}_${proto.hashCode()}"), date) {
        override fun toString(): String {
            return "UnknownEvent(date=$date, persona=$persona, type=$type, proto=$proto)"
        }
    }
}