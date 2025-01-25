package bruhcollective.itaysonlab.ksteam.models.enums

enum class ECollectionPlayState {
    InstalledLocally,
    ReadyToPlay,
    PlayedPreviously,
    PlayedNever,
    ValidPlatform;

    companion object {
        fun byIndex(index: Int) = entries.getOrNull(index - 1)
    }
}