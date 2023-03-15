package bruhcollective.itaysonlab.ksteam.models.enums

enum class EClientPersonaStateFlag (val mask: Int) {
    Status(1),
    PlayerName(2),
    QueryPort(4),
    SourceID(8),
    Presence(16),
    LastSeen(64),
    UserClanRank(128),
    GameExtraInfo(256),
    GameDataBlob(512),
    ClanData(1024),
    Facebook(2048),
    RichPresence(4096),
    Broadcast(8192),
    Watching(16384);

    companion object {
        val Default = Status + PlayerName + Presence + LastSeen + GameExtraInfo + RichPresence
    }
}

operator fun Int.plus(flag: EClientPersonaStateFlag) = this or flag.mask
operator fun EClientPersonaStateFlag.plus(flag: EClientPersonaStateFlag) = this.mask or flag.mask