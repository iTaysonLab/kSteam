package bruhcollective.itaysonlab.ksteam.models.persona

import androidx.compose.runtime.Immutable
import bruhcollective.itaysonlab.ksteam.cdn.CommunityAppImageUrl
import bruhcollective.itaysonlab.ksteam.models.apps.AppSummary
import steam.webui.player.EProfileCustomizationType

@Immutable
sealed class ProfileWidget {
    @Immutable
    data class FavoriteGame internal constructor(
        val app: AppSummary,
        val achievementProgress: AchievementProgress,
        val playedSeconds: Int
    ): ProfileWidget() {
        @Immutable
        data class AchievementProgress internal constructor(
            val totalAchievements: Int,
            val currentAchivements: Int,
            val topPictures: List<CommunityAppImageUrl>
        )
    }

    @Immutable
    data class GameCollector internal constructor(
        val featuredApps: List<AppSummary>,
        val ownedGamesCount: Int
    ): ProfileWidget()

    /**
     * Unknown to kSteam widget
     */
    @Immutable
    data class Unknown internal constructor(
        val type: EProfileCustomizationType
    ): ProfileWidget()
}