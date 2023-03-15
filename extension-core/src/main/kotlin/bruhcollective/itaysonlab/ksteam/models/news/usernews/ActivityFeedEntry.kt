package bruhcollective.itaysonlab.ksteam.models.news.usernews

import androidx.compose.runtime.Immutable
import bruhcollective.itaysonlab.ksteam.cdn.CommunityAppImageUrl
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.apps.AppSummary
import bruhcollective.itaysonlab.ksteam.models.enums.EUserNewsType
import bruhcollective.itaysonlab.ksteam.models.news.NewsEntry
import bruhcollective.itaysonlab.ksteam.models.persona.Persona
import steam.webui.usernews.CUserNews_Event

sealed class ActivityFeedEntry (
    val date: Int,
    val steamId: SteamId,
    val persona: Persona,
) {
    /**
     * A new blog post was posted about this app.
     */
    @Immutable
    class PostedAnnouncement(
        date: Int,
        steamId: SteamId,
        persona: Persona,
        val app: AppSummary,
        val announcement: bruhcollective.itaysonlab.ksteam.models.news.NewsEntry
    ): ActivityFeedEntry(date, steamId, persona)

    /**
     * A user now owns these games from the list.
     */
    @Immutable
    class ReceivedNewGame(
        date: Int,
        steamId: SteamId,
        persona: Persona,
        val apps: List<AppSummary>
    ): ActivityFeedEntry(date, steamId, persona)

    /**
     * A user played the game for the first time.
     */
    @Immutable
    class PlayedForFirstTime(
        date: Int,
        steamId: SteamId,
        persona: Persona,
        val app: AppSummary
    ): ActivityFeedEntry(date, steamId, persona)

    /**
     * A user has received new achievements in this game.
     */
    @Immutable
    class NewAchievements(
        date: Int,
        steamId: SteamId,
        persona: Persona,
        val app: AppSummary,
        val achievements: List<Achievement>
    ): ActivityFeedEntry(date, steamId, persona) {
        class Achievement(
            val internalName: String,
            val displayName: String,
            val displayDescription: String,
            val icon: CommunityAppImageUrl,
            val unlockedPercent: Double,
            val hidden: Boolean
        )
    }

    /**
     * kSteam does not know about this event.
     */
    @Immutable
    class UnknownEvent(
        date: Int,
        steamId: SteamId,
        persona: Persona,
        val type: EUserNewsType,
        val proto: CUserNews_Event
    ): ActivityFeedEntry(date, steamId, persona)
}