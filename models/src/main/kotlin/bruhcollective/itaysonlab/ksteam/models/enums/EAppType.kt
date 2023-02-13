package bruhcollective.itaysonlab.ksteam.models.enums

enum class EAppType (internal val bitMask: Int) {
    Invalid(0),
    Game(1),
    Application(2),
    Tool(4),
    Demo(8),
    Deprecated(16),
    DLC(32),
    Guide(64),
    Driver(128),
    Config(256),
    Hardware(512),
    Franchise(1024),
    Video(2048),
    Plugin(4096),
    MusicAlbum(8192),
    Series(16384),
    Comic(32768),
    Beta(65536),
    Shortcut(1073741824),
    DepotOnly(Int.MIN_VALUE);

    companion object {
        fun byBitMask(mask: Int) = values().firstOrNull { it.bitMask == mask }
    }
}