package bruhcollective.itaysonlab.ksteam.database.exposed

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.api.ExposedBlob

internal fun ExpressionWithColumnType<ExposedBlob>.expand() = function("EXPAND")

internal class H2Compress(
    compressWhat: ExposedBlob,
    useDeflate: Boolean = false
): CustomFunction<ExposedBlob>(
    functionName = "COMPRESS", columnType = BlobColumnType(), QueryParameter(compressWhat, BlobColumnType()), stringParam(if (useDeflate) "DEFLATE" else "LZF")
)