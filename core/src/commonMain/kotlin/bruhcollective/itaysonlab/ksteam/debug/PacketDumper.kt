package bruhcollective.itaysonlab.ksteam.debug

import bruhcollective.itaysonlab.ksteam.handlers.Logger
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.messages.SteamPacketHeader
import kotlinx.atomicfu.atomic
import kotlinx.datetime.Clock
import okio.FileSystem
import okio.Path
import okio.SYSTEM

/**
 * This is a simple dumper infrastructure which can save Steam3 packets in a NetHook-style files.
 */
class PacketDumper internal constructor(
    saveRootFolder: Path,
    private val logger: Logger
) {
    private val sessionFolder = saveRootFolder / "dumps" / Clock.System.now().toEpochMilliseconds().toString()
    private val loggedPacketIndex = atomic(0)

    var mode: DumpMode = DumpMode.Disable

    fun onPacket(packet: SteamPacket, directionOut: Boolean) {
        if (mode == DumpMode.Disable) return

        val direction = if (directionOut) "out" else "in"

        logger.logVerbose("PacketDumper") {
            when (val header = packet.header) {
                is SteamPacketHeader.Protobuf -> {
                    if (header.targetJobId != 0L) {
                        "[$direction] ${packet.messageId} [protobuf, result = ${header.result}, job = ${header.targetJobName}]"
                    } else {
                        "[$direction] ${packet.messageId} [protobuf]"
                    }
                }

                is SteamPacketHeader.Binary -> {
                    "[$direction] ${packet.messageId} [binary]"
                }
            }
        }

        FileSystem.SYSTEM.apply {
            createDirectories(sessionFolder, mustCreate = false)

            write(file = sessionFolder / "${loggedPacketIndex.getAndIncrement()}_${direction}_${packet.messageId.name.removePrefix("k_")}.bin", mustCreate = true) {
                write(packet.encode())
            }
        }
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