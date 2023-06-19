package bruhcollective.itaysonlab.ksteam.models.persona

import bruhcollective.itaysonlab.ksteam.models.apps.AppSummary
import bruhcollective.itaysonlab.ksteam.platform.Immutable
import steam.enums.EProfileCustomizationType

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
            val currentAchievements: Int,
            val topPictures: List<String>
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