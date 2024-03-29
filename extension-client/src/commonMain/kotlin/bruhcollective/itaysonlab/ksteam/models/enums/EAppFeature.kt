package bruhcollective.itaysonlab.ksteam.models.enums

enum class EAppFeature {
    FullControllerSupport,
    PartialControllerSupport,
    VRSupport,
    TradingCards,
    Workshop,
    Achievements,
    SinglePlayer,
    MultiPlayer,
    CoOp,
    Cloud,
    RemotePlayTogether,
    SteamDeckVerified,
    SteamDeckPlayable,
    SteamDeckUnknown,
    SteamDeckUnsupported;

    companion object {
        fun byIndex(index: Int) = entries.getOrNull(index - 1)
    }
}