package bruhcollective.itaysonlab.ksteam.models.enums

enum class EPersonaState {
    Offline,
    Online,
    Busy,
    Away,
    Snooze,
    LookingToTrade,
    LookingToPlay;

    companion object {
        fun byEncoded(num: Int) = entries.getOrElse(num) { Offline }
    }
}