package bruhcollective.itaysonlab.ksteam.platform

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.json.okio.encodeToBufferedSink
import kotlinx.serialization.serializer
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import kotlin.reflect.KProperty

class FileProxiedObject <T> (
    private val fileRef: Path,
    private val serializer: KSerializer<T>,
    private val default: T
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val initialContents = FileSystem.SYSTEM.let { fs ->
        if (fs.exists(fileRef).not()) {
            // regenerate the file
            commit(default)
            default
        } else {
            fs.read(fileRef) {
                json.decodeFromBufferedSource(serializer,this)
            }
        }
    }

    var value: T = initialContents
        set(value) {
            field = value
            commit(value)
        }

    @OptIn(ExperimentalSerializationApi::class)
    private fun commit(data: T) {
        FileSystem.SYSTEM.apply {
            fileRef.parent?.let { createDirectories(it) }

            write(fileRef) {
                json.encodeToBufferedSink(serializer, data, this)
            }
        }
    }
}

inline fun <reified T> fileProxiedObject(path: Path, default: T) = FileProxiedObject<T>(fileRef = path, serializer = serializer(), default = default)

operator fun <T> FileProxiedObject<T>.getValue(thisObj: Any?, property: KProperty<*>): T {
    return value
}

operator fun <T> FileProxiedObject<T>.setValue(thisObj: Any?, property: KProperty<*>, value: T) {
    this.value = value
}