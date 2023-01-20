package bruhcollective.itaysonlab.ksteam.models.enums

enum class EUniverse {
    Invalid,
    Public,
    Beta,
    Internal,
    Dev;

    companion object {
        fun byEncoded(num: Int) = values().getOrElse(num) { Invalid }
    }
}