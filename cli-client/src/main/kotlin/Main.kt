
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import okio.ByteString.Companion.toByteString
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.security.Security

fun main(args: Array<String>) {
    Security.addProvider(BouncyCastleProvider())

    println("kSteam CLI testing utility")
    println("- - Options - -")
    println("> run - starts a kSteam client instance")
    println("> dump - print kSteam PacketDumper packet information")
    println("- - - - - - - -")

    when (readln()) {
        "run" -> {
            KSteamClient().start()
        }

        "dump" -> {
            println("Enter a full path to a packet file (usually ending with .bin):")

            val packet = SteamPacket.ofNetworkPacket(File(readln()).readBytes())

            println("- - Information - -")
            println("- Message ID: ${packet.messageId.name.removePrefix("k_")}")
            println("- Is Protobuf: ${packet.isProtobuf()}")

            println("- - Header - -")
            println(packet.header)

            println("- - Payload - -")
            println(packet.payload.toByteString().hex().chunked(512).joinToString("\n"))
        }
    }



    //
}