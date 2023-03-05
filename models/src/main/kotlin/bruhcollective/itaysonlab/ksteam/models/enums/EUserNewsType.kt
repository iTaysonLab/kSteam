package bruhcollective.itaysonlab.ksteam.models.enums

enum class EUserNewsType (internal val mask: Int, internal val apiEnum: Int) {
    FriendAdded(1, 1),
    AchievementUnlocked(2, 2),
    ReceivedNewGame(4, 3),
    PlayedGameFirstTime(4, 30),
    JoinedGroup(8, 4),
    AddedGameToWishlist(128, 9),
    RecommendedGame(256, 10),
    ScreenshotPublished_Deprecated(512, 11),
    VideoPublished_Deprecated(1024, 12),
    FilePublished_Screenshot(512, 13),
    FilePublished_Video(1024, 14),
    FilePublished_WorkshopItem(8192, 15),
    UserStatus(4096, 16),
    FilePublished_Collection(8192, 17),
    FilePublished_WebGuide(8192, 20),
    FilePublished_Art(8192, 22),
    ClanAchievement(65536, 1001),
    PostedAnnouncement(131072, 1002),
    ScheduledEvent(262144, 1003),
    SelectedNewPOTW(524288, 1004),
    PromotedNewAdmin(1048576, 1005),
    MessageOnClanPage(2097152, 1006),
    CuratorRecommendedGame(4194304, 1007),
    FileFavorited(16384, 23),

    // Non-bitmasked
    CommentByMe(0, 5),
    FriendRemoved(0, 6),
    GroupCreated(0, 7),
    CommentOnMe(0, 8),
    FilePublished_GreenlightGame(0, 18),
    FilePublished_WorkshopAnnouncement(0, 19),
    FilePublished_Screenshot_Tagged(0, 21);

    companion object {
        fun byApiEnum(apiEnum: Int) = EUserNewsType.values().firstOrNull { it.apiEnum == apiEnum }
    }
}

operator fun Int.plus(flag: EUserNewsType) = this or flag.mask
operator fun EUserNewsType.plus(flag: EUserNewsType) = this.mask or flag.mask