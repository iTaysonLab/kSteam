package bruhcollective.itaysonlab.ksteam.models.enums

enum class EAppFeature {
    Ignored,
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
    SteamDeckUnsupported,
    PS4ControllerSupport,
    PS4ControllerBTSupport,
    PS5ControllerSupport,
    PS5ControllerBTSupport,
    SteamInputAPI,
    GamepadPreferred,
    HDR,
    FamilySharing;

    companion object {
        fun byIndex(index: Int) = entries.getOrNull(index)
    }
}