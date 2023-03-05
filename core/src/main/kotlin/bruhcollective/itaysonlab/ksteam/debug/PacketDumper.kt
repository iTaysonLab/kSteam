package bruhcollective.itaysonlab.ksteam.debug

import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import java.io.File

/**
 * This is a simple dumper infrastructure which can save Steam3 packets in a NetHook-style files.
 */
class PacketDumper internal constructor(
    saveRootFolder: File
) {
    private val sessionFolder = File(File(saveRootFolder, "dumps"), System.currentTimeMillis().toString())

    var dumpMode: DumpMode = DumpMode.Disable
    private var loggedPacketIndex: Int = 0

    fun onPacket(packet: SteamPacket, directionOut: Boolean) {
        if (dumpMode == DumpMode.Disable) return

        sessionFolder.mkdirs()

        File(sessionFolder, "${loggedPacketIndex}_${if (directionOut) "out" else "in"}_${packet.messageId.name.removePrefix("k_")}.bin").also {
            it.createNewFile()
        }.writeBytes(packet.encode())

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