package bruhcollective.itaysonlab.ksteam.models.news

/**
 * Represents an event type.
 */
enum class NewsEventType {
    Unknown, // padding to ensure ordinals match the Steam definition
    Other,
    Game,
    Party,
    Meeting,
    SpecialCause,
    MusicAndArts,
    Sports,
    Trip,
    Chat,
    GameRelease,
    Broadcast,
    SmallUpdate,
    PreAnnounceMajorUpdate,
    MajorUpdate,
    DLCRelease,
    FutureRelease,
    ESportTournamentStream,
    DevStream,
    FamousStream,
    GameSales,
    GameItemSales,
    InGameBonusXP,
    InGameLoot,
    InGamePerks,
    InGameChallenge,
    InGameContest,
    IRL,
    News,
    BetaRelease,
    InGameContentRelease,
    FreeTrial,
    SeasonRelease,
    SeasonUpdate,
    Crosspost,
    InGameEventGeneral;

    /**
     * A collection of types for usage in request methods.
     *
     * Lists are taken from the Steam JS libraries.
     */
    object Collections {
        // News
        val OnlyNews = listOf(News)

        // Events
        val Events = listOf(Chat, IRL, InGameBonusXP, InGameLoot, InGamePerks, InGameChallenge, InGameContest, InGameEventGeneral)

        // Live-Streams
        val Streaming = listOf(Broadcast)

        // Content Updates
        val Updates = listOf(SmallUpdate, PreAnnounceMajorUpdate, MajorUpdate)

        // Releases
        val Releases = listOf(GameRelease, BetaRelease, FutureRelease, DLCRelease, SeasonRelease)

        // Sales
        val Sales = listOf(GameSales, GameItemSales, FreeTrial, Crosspost)

        val Everything = OnlyNews + Events + Streaming + Updates + Releases + Sales
    }
}