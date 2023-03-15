package bruhcollective.itaysonlab.ksteam.models.enums

enum class EPersonaStateFlag (val mask: Int) {
    HasRichPresence(1),
    InJoinableGame(2),
    Golden(4),
    RemotePlayTogether(8),
    ClientTypeWeb(256),
    ClientTypeMobile(512),
    ClientTypeTenfoot(1024),
    ClientTypeVR(2048),
    LaunchTypeGamepad(4096),
    LaunchTypeCompatTool(8192)
}

operator fun Int.plus(flag: EPersonaStateFlag) = this or flag.mask
operator fun EPersonaStateFlag.plus(flag: EPersonaStateFlag) = this.mask or flag.mask