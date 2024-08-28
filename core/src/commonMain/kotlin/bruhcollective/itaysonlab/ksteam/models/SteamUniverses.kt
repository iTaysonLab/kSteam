package bruhcollective.itaysonlab.ksteam.models

enum class SteamInstance (val apiRepresentation: Int) {
    Other(0),
    SteamUserDesktop(1),
    SteamUserConsole(2),
    SteamUserWeb(4);

    companion object {
        fun byApiRepresentation(value: Int) = SteamInstance.entries.firstOrNull { it.apiRepresentation == value } ?: Other
    }
}