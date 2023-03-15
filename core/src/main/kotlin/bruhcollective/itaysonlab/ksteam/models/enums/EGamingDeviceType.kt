package bruhcollective.itaysonlab.ksteam.models.enums

enum class EGamingDeviceType (val encoded: Int) {
    k_EGamingDeviceType_Unknown(0),
    k_EGamingDeviceType_StandardPC(1),
    k_EGamingDeviceType_Console(256),
    k_EGamingDeviceType_PS3(272),
    k_EGamingDeviceType_Steambox(288),
    k_EGamingDeviceType_Handheld(512),
    k_EGamingDeviceType_Phone(528),
    k_EGamingDeviceType_SteamDeck(544);

    companion object {
        fun byEncoded(num: Int?) = num?.let { n -> values().firstOrNull { it.encoded == n } } ?: k_EGamingDeviceType_Unknown
    }
}