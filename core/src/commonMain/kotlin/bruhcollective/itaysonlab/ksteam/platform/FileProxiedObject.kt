package bruhcollective.itaysonlab.ksteam.platform

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.json.okio.encodeToBufferedSink
import kotlinx.serialization.serializer
import okio.Path
import okio.Path.Companion.toPath

class FileProxiedObject <T> (
    private val fileRef: Path,
    private val serializer: KSerializer<T>,
    private val default: T
) {
    private val okioFs = provideOkioFilesystem()

    @OptIn(ExperimentalSerializationApi::class)
    private val initialContents = okioFs.let { fs ->
        if (fs.exists(fileRef).not()) {
            // regenerate the file
            commit(default)
            default
        } else {
            fs.read(fileRef) {
                Json.decodeFromBufferedSource(serializer,this)
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
        okioFs.apply {
            createDirectories(fileRef)

            write(fileRef) {
                Json.encodeToBufferedSink(serializer, data, this)
            }
        }
    }
}

inline fun <reified T> fileProxiedObject(fileName: String, default: T) = FileProxiedObject<T>(fileRef = FileProxiedObject.FPO_CONFIG_ROOT / fileName.toPath(), serializer = serializer(), default = default)