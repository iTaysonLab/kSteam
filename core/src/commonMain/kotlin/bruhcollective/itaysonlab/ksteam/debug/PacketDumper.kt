package bruhcollective.itaysonlab.ksteam.debug

import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.platform.provideOkioFilesystem
import kotlinx.datetime.Clock
import okio.Path

/**
 * This is a simple dumper infrastructure which can save Steam3 packets in a NetHook-style files.
 */
class PacketDumper internal constructor(
    saveRootFolder: Path
) {
    private val sessionFolder = saveRootFolder / "dumps" / Clock.System.now().epochSeconds.toString()

    var dumpMode: DumpMode = DumpMode.Disable
    private var loggedPacketIndex: Int = 0

    fun onPacket(packet: SteamPacket, directionOut: Boolean) {
        if (dumpMode == DumpMode.Disable) return

        provideOkioFilesystem().createDirectories(sessionFolder, mustCreate = false)

        provideOkioFilesystem().write(
            file = sessionFolder / "${loggedPacketIndex}_${if (directionOut) "out" else "in"}_${packet.messageId.name.removePrefix("k_")}.bin",
            mustCreate = true
        ) {
            write(packet.encode())
        }

        loggedPacketIndex++
    }

    /**
     * The dump mode.
     */
    enum class DumpMode {
        /**
         * Save everything, including the packet header.
         * Format: ksteam/dumps/%session_start_date%/%num%_(in/out)_%message_name%.bin
         */
        Full,

        /**
         * Don't dump anything.
         */
        Disable
    }
}