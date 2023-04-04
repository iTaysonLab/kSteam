package bruhcollective.itaysonlab.ksteam.models.persona

import bruhcollective.itaysonlab.ksteam.cdn.CommunityAppImageUrl
import bruhcollective.itaysonlab.ksteam.models.apps.AppSummary
import steam.webui.player.EProfileCustomizationType

sealed class ProfileWidget {
    data class FavoriteGame internal constructor(
        val app: AppSummary,
        val achievementProgress: AchievementProgress,
        val playedSeconds: Int
    ): ProfileWidget() {
        data class AchievementProgress internal constructor(
            val totalAchievements: Int,
            val currentAchivements: Int,
            val topPictures: List<CommunityAppImageUrl>
        )
    }

    data class GameCollector internal constructor(
        val featuredApps: List<AppSummary>,
        val ownedGamesCount: Int
    ): ProfileWidget()

    /**
     * Unknown to kSteam widget
     */
    data class Unknown internal constructor(
        val type: EProfileCustomizationType
    ): ProfileWidget()
}