package bruhcollective.itaysonlab.ksteam.models.enums

enum class EPlayState {
    InstalledLocally,
    ReadyToPlay,
    PlayedPreviously,
    PlayedNever,
    ValidPlatform;

    companion object {
        fun byIndex(index: Int) = values().getOrNull(index + 1)
    }
}