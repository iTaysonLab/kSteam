package bruhcollective.itaysonlab.ksteam.database.keyvalue

/**
 * Expose your key-value DB solution here.
 *
 * On Android, MMKV can be used. On other platforms - MMKV or LevelDB with JNI.
 */
interface KeyValueDatabase {
    fun getAllByteArrays(startingFrom: String): List<ByteArray>
    fun putByteArrays(values: Map<String, ByteArray>)

    fun putByteArray(key: String, value: ByteArray)
    fun getByteArray(key: String): ByteArray

    fun putLong(key: String, value: Long)
    fun getLong(key: String): Long
}

internal object NoopKeyValueDatabase: KeyValueDatabase {
    override fun getAllByteArrays(startingFrom: String): List<ByteArray> {
        TODO("Not yet implemented")
    }

    override fun putByteArrays(values: Map<String, ByteArray>) {
        TODO("Not yet implemented")
    }

    override fun putByteArray(key: String, value: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun getByteArray(key: String): ByteArray {
        TODO("Not yet implemented")
    }

    override fun putLong(key: String, value: Long) {
        TODO("Not yet implemented")
    }

    override fun getLong(key: String): Long {
        TODO("Not yet implemented")
    }

}