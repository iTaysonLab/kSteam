package bruhcollective.itaysonlab.ksteam.util

// https://www.baeldung.com/java-byte-arrays-hex-strings
object HexNumbers {
    fun toByteArray(hexString: String): ByteArray {
        if (hexString.length % 2 == 1) {
            throw IllegalArgumentException("Invalid hexadecimal String supplied.")
        }

        return ByteArray(hexString.length / 2).also { bytes ->
            for (i in hexString.indices step 2) {
                bytes[i / 2] = hexToByte(hexString.substring(i, i + 2))
            }
        }
    }

    private fun hexToByte(hexString: String): Byte {
        val firstDigit = toDigit(hexString[0])
        val secondDigit = toDigit(hexString[1])
        return ((firstDigit shl 4) + secondDigit).toByte()
    }

    private fun toDigit(hexChar: Char): Int {
        return CharacterDataLatin1.digit(hexChar, 16).also {
            require(it != -1) { "Invalid Hexadecimal Character: $hexChar" }
        }
    }
}