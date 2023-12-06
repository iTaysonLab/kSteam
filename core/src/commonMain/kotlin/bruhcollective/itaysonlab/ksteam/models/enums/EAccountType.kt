package bruhcollective.itaysonlab.ksteam.models.enums

enum class EAccountType {
    Invalid,
    Individual,
    Multiseat,
    GameServer,
    AnonGameServer,
    Pending,
    ContentServer,
    Clan,
    Chat,
    ConsoleUser,
    AnonUser;

    companion object {
        fun byEncoded(num: Int) = entries.getOrElse(num) { Invalid }
    }
}