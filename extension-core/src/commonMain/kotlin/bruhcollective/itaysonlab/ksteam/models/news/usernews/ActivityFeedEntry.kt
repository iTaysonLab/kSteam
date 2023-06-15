package bruhcollective.itaysonlab.ksteam.models.news.usernews

import bruhcollective.itaysonlab.ksteam.models.apps.AppSummary
import bruhcollective.itaysonlab.ksteam.models.enums.EUserNewsType
import bruhcollective.itaysonlab.ksteam.models.news.NewsEvent
import bruhcollective.itaysonlab.ksteam.models.persona.SummaryPersona
import bruhcollective.itaysonlab.ksteam.platform.Immutable
import steam.webui.usernews.CUserNews_Event
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Immutable
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
     * A synthetic entry which holds a [NewsEvent]. Useful for unified feeds.
     */
    @Immutable
    class NewsfeedEvent(
        date: Int,
        val event: NewsEvent
    ): ActivityFeedEntry(id = event.id, date) {
        override fun toString(): String {
            return "NewsfeedEvent(date=$date, event=$event"
        }
    }

    /**
     * A user now owns these games from the list.
     */
    @Immutable
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
     * A user added some games to their wishlist.
     */
    @Immutable
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
    @Immutable
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
    @Immutable
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
     * kSteam does not know about this event.
     */
    @Immutable
    class UnknownEvent(
        date: Int,
        val persona: SummaryPersona,
        val type: EUserNewsType,
        val proto: CUserNews_Event
    ): ActivityFeedEntry(id = buildId(date, persona, "unk_${proto.hashCode()}"), date) {
        override fun toString(): String {
            return "UnknownEvent(date=$date, persona=$persona, type=$type, proto=$proto)"
        }
    }
}