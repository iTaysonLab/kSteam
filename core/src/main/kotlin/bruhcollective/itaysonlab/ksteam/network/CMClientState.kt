package bruhcollective.itaysonlab.ksteam.network

enum class CMClientState {
    Idle,
    Connecting,
    Reconnecting,
    Logging,
    Connected,
    Error
}