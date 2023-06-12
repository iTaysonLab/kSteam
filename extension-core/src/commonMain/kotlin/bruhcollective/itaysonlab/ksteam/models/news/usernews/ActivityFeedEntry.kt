package bruhcollective.itaysonlab.ksteam.models.news.usernews

import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.apps.AppSummary
import bruhcollective.itaysonlab.ksteam.models.enums.EUserNewsType
import bruhcollective.itaysonlab.ksteam.models.news.NewsEntry
import bruhcollective.itaysonlab.ksteam.models.persona.SummaryPersona
import bruhcollective.itaysonlab.ksteam.platform.Immutable
import steam.webui.usernews.CUserNews_Event
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Immutable
sealed class ActivityFeedEntry (
    val date: Int,
    val steamId: SteamId,
    val persona: SummaryPersona,
) {
    companion object {
        @OptIn(ExperimentalContracts::class)
        fun defaultMatch(event: CUserNews_Event, eventNext: CUserNews_Event?, type: EUserNewsType): Boolean {
            contract {
                returns(true) implies (eventNext != null)
            }

            return eventNext != null && eventNext.eventtime == event.eventtime && eventNext.eventtype == type.apiEnum && eventNext.steamid_actor == event.steamid_actor
        }
    }

    /**
     * A new blog post was posted about this app.
     */
    @Immutable
    class PostedAnnouncement(
        date: Int,
        steamId: SteamId,
        persona: SummaryPersona,
        val announcement: NewsEntry
    ): ActivityFeedEntry(date, steamId, persona) {
        override fun toString(): String {
            return "PostedAnnouncement(date=$date, steamId=$steamId, persona=$persona, announcement=$announcement)"
        }
    }

    /**
     * A user now owns these games from the list.
     */
    @Immutable
    class ReceivedNewGame(
        date: Int,
        steamId: SteamId,
        persona: SummaryPersona,
        val apps: List<AppSummary>,
        val packages: List<AppSummary>,
    ): ActivityFeedEntry(date, steamId, persona) {
        companion object {
            fun canMergeWith(event: CUserNews_Event, eventNext: CUserNews_Event?) = defaultMatch(event, eventNext, EUserNewsType.ReceivedNewGame)
        }

        override fun toString(): String {
            return "ReceivedNewGame(date=$date, steamId=$steamId, persona=$persona, apps=${apps.joinToString()}, packages=${packages.joinToString()})"
        }
    }

    /**
     * A user added some games to their wishlist.
     */
    @Immutable
    class AddedToWishlist(
        date: Int,
        steamId: SteamId,
        persona: SummaryPersona,
        val apps: List<AppSummary>
    ): ActivityFeedEntry(date, steamId, persona) {
        companion object {
            fun canMergeWith(event: CUserNews_Event, eventNext: CUserNews_Event?) = defaultMatch(event, eventNext, EUserNewsType.AddedGameToWishlist)
        }

        override fun toString(): String {
            return "AddedToWishlist(date=$date, steamId=$steamId, persona=$persona, apps=${apps.joinToString()})"
        }
    }

    /**
     * A user played the game for the first time.
     */
    @Immutable
    class PlayedForFirstTime(
        date: Int,
        steamId: SteamId,
        persona: SummaryPersona,
        val app: AppSummary
    ): ActivityFeedEntry(date, steamId, persona) {
        override fun toString(): String {
            return "PlayedForFirstTime(date=$date, steamId=$steamId, persona=$persona, app=$app)"
        }
    }

    /**
     * A user has received new achievements in this game.
     */
    @Immutable
    class NewAchievements(
        date: Int,
        steamId: SteamId,
        persona: SummaryPersona,
        val app: AppSummary,
        val achievements: List<Achievement>
    ): ActivityFeedEntry(date, steamId, persona) {
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
            return "NewAchievements(date=$date, steamId=$steamId, persona=$persona, app=$app, achievements=[${achievements.joinToString()}])"
        }
    }

    /**
     * kSteam does not know about this event.
     */
    @Immutable
    class UnknownEvent(
        date: Int,
        steamId: SteamId,
        persona: SummaryPersona,
        val type: EUserNewsType,
        val proto: CUserNews_Event
    ): ActivityFeedEntry(date, steamId, persona) {
        override fun toString(): String {
            return "UnknownEvent(date=$date, steamId=$steamId, persona=$persona, type=$type, proto=$proto)"
        }
    }
}