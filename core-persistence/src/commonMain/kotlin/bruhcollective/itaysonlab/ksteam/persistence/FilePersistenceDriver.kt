package bruhcollective.itaysonlab.ksteam.persistence

import bruhcollective.itaysonlab.ksteam.models.SteamId
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.json.okio.encodeToBufferedSink
import okio.FileSystem
import okio.Path

/**
 * Defines a kSteam persistence driver using a file located in the kSteam working directory.
 *
 * **WARNING: THIS DOES NOT ENCRYPT SECURE DATA!** This includes Steam Guard and Authorization information.
 *
 * @param path location of a persistence file
 */
class FilePersistenceDriver (
    private val fileSystem: FileSystem,
    private val path: Path
): KsteamPersistenceDriver {
    private var futureJsonObject = mutableMapOf<String, JsonElement>()

    init { readJson() }

    override fun getString(key: String): String? {
        return futureJsonObject[key]?.jsonPrimitive?.contentOrNull
    }

    override fun getLong(key: String): Long {
        return futureJsonObject[key]?.jsonPrimitive?.longOrNull ?: 0L
    }

    override fun getInt(key: String): Int {
        return futureJsonObject[key]?.jsonPrimitive?.intOrNull ?: 0
    }

    override fun set(key: String, value: String) {
        futureJsonObject[key] = JsonPrimitive(value)
        writeJson()
    }

    override fun set(key: String, value: Long) {
        futureJsonObject[key] = JsonPrimitive(value)
        writeJson()
    }

    override fun set(key: String, value: Int) {
        futureJsonObject[key] = JsonPrimitive(value)
        writeJson()
    }

    override fun containsKey(key: String): Boolean {
        return futureJsonObject.containsKey(key)
    }

    override fun delete(vararg key: String) {
        key.forEach(futureJsonObject::remove)
        writeJson()
    }

    override fun secureGetSteamIds(): List<SteamId> {
        return futureJsonObject.keys.mapNotNull {
            it.split(".").getOrNull(1)?.toULongOrNull()?.let(::SteamId)
        }.distinct()
    }

    override fun secureGet(id: SteamId, key: String): String? {
        return futureJsonObject[secureIdKey(id, key)]?.jsonPrimitive?.contentOrNull
    }

    override fun secureSet(id: SteamId, key: String, value: String) {
        futureJsonObject[secureIdKey(id, key)] = JsonPrimitive(value)
        writeJson()
    }

    override fun secureSet(id: SteamId, vararg pairs: Pair<String, String>) {
        pairs.forEach { pair ->
            futureJsonObject[secureIdKey(id, pair.first)] = JsonPrimitive(pair.second)
        }

        writeJson()
    }

    override fun secureDelete(id: SteamId, vararg key: String) {
        key.map { secureIdKey(id, it) }.forEach(futureJsonObject::remove)
        writeJson()
    }

    override fun secureContainsKey(id: SteamId, key: String): Boolean {
        return futureJsonObject.containsKey(secureIdKey(id, key))
    }

    //

    private fun secureIdKey(id: SteamId, key: String): String {
        return "secure.${id}.${key}"
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun readJson() {
        if (fileSystem.exists(path)) {
            fileSystem.read(path) {
                futureJsonObject = Json.decodeFromBufferedSource<JsonObject>(this).toMutableMap()
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun writeJson() {
        fileSystem.write(path) {
            Json.encodeToBufferedSink(JsonObject(futureJsonObject), this)
        }
    }
}
