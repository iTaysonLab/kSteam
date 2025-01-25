package bruhcollective.itaysonlab.ksteam.models.persona

import bruhcollective.itaysonlab.ksteam.models.apps.AppSummary
import steam.enums.EProfileCustomizationType

sealed interface ProfileWidget {
    data class FavoriteGame (
        val app: AppSummary,
        val achievementProgress: AchievementProgress,
        val playedSeconds: Int
    ): ProfileWidget {
        data class AchievementProgress (
            val totalAchievements: Int,
            val currentAchievements: Int,
            val topPictures: List<String>
        )
    }

    data class GameCollector (
        val featuredApps: List<AppSummary>,
        val ownedGamesCount: Int
    ): ProfileWidget

    /**
     * Unknown to kSteam widget
     */
    data class Unknown (
        val type: EProfileCustomizationType
    ): ProfileWidget
}