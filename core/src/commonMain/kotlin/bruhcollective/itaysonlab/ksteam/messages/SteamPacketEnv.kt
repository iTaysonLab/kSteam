package bruhcollective.itaysonlab.ksteam.messages

/**
 * Mini-copy of current connected environment for SteamPacket creation.
 */
class SteamPacketEnv (
    val steamId: ULong,
    val sessionId: Int
)