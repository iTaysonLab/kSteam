package bruhcollective.itaysonlab.ksteam.models.enums

import bruhcollective.itaysonlab.ksteam.models.enums.ECollectionAppType.entries

enum class ECollectionAppType (internal val bitMask: Int, internal val vdfName: String) {
    Invalid(0, ""),
    Game(1, "Game"),
    Application(2, "Application"),
    Tool(4, "Tool"),
    Demo(8, "Demo"),
    Deprecated(16, ""),
    DLC(32, "DLC"),
    Guide(64, "Guide"),
    Driver(128, "Driver"),
    Config(256, "Config"),
    Hardware(512, "Hardware"),
    Franchise(1024, "Franchise"),
    Video(2048, "Video"),
    Plugin(4096, "Plugin"),
    MusicAlbum(8192, "Music"),
    Series(16384, "Series"),
    Comic(32768, "Comic"),
    Beta(65536, "Beta"),
    Shortcut(1073741824, "Shortcut"),
    DepotOnly(Int.MIN_VALUE, "Depot");

    companion object {
        fun byBitMask(mask: Int) = entries.firstOrNull { e -> e.bitMask == mask }
        fun byVdfName(vdfName: String) = entries.firstOrNull { e -> e.vdfName.equals(vdfName, ignoreCase = true) }
    }
}