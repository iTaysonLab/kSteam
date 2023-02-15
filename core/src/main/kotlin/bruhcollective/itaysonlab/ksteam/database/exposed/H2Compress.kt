package bruhcollective.itaysonlab.ksteam.database.exposed

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.api.ExposedBlob

fun ExpressionWithColumnType<ExposedBlob>.expand() = function("EXPAND")

class H2Compress(
    compressWhat: ExposedBlob,
    useDeflate: Boolean = false
): CustomFunction<ExposedBlob>(
    functionName = "COMPRESS", columnType = BlobColumnType(), LiteralOp(BlobColumnType(), compressWhat), stringParam(if (useDeflate) "DEFLATE" else "LZF")
)