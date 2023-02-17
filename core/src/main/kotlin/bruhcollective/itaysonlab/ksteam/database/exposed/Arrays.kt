package bruhcollective.itaysonlab.ksteam.database.exposed

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.jetbrains.exposed.sql.statements.jdbc.JdbcPreparedStatementImpl
import kotlin.Array
import java.sql.Array as SQLArray

// Taken from https://gist.github.com/DRSchlaubi/cb146ee2b4d94d1c89b17b358187c612 with some details improved
// Tweaked to support specific H2 Database features

/**
 * Creates an array column with [name].
 *
 * @param size an optional size of the array
 */
internal fun <T : Comparable<T>> Column<T>.array(size: Int? = null): Column<Array<T>> {
    return table.replaceColumn(this, Column(table, name, ArrayColumnType(columnType.sqlType(), size)))
}

internal infix fun Expression<Array<Int>>.arrayContains(other: Int) = arrayContains(intLiteral(other))
internal infix fun Expression<Array<String>>.arrayContains(other: String) = arrayContains(stringLiteral(other))
internal infix fun <T> Expression<Array<T>>.arrayContains(other: LiteralOp<T>) = ArrayContains(this, other)

internal class ArrayContains <T> (
    private val targetArray: Expression<Array<T>>,
    private val containsWhat: Expression<T>,
): Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        append("ARRAY_CONTAINS(")
        append(targetArray)
        append(", ")
        append(containsWhat)
        append(")")
    }
}

/**
 * Implementation of [ColumnType] for the SQL `ARRAY` type.
 *
 * @property underlyingType the type of the array
 * @property size an optional size of the array
 */
internal class ArrayColumnType(
    private val underlyingType: String, private val size: Int?
) : ColumnType() {
    override fun sqlType(): String = "$underlyingType ARRAY${size?.let { "[$it]" } ?: ""}"

    override fun notNullValueToDB(value: Any): Any = when (value) {
        is Array<*> -> value
        is Collection<*> -> value.toTypedArray()
        else -> error("Got unexpected array value of type: ${value::class.qualifiedName} ($value)")
    }

    override fun valueFromDB(value: Any): Any = when (value) {
        is SQLArray -> value.array as Array<*>
        is Array<*> -> value
        is Collection<*> -> value.toTypedArray()
        else -> error("Got unexpected array value of type: ${value::class.qualifiedName} ($value)")
    }

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        if (value == null) {
            stmt.setNull(index, this)
        } else {
            val preparedStatement = stmt as? JdbcPreparedStatementImpl ?: error("Currently only JDBC is supported")
            val array = preparedStatement.statement.connection.createArrayOf(underlyingType, value as Array<*>)
            stmt[index] = array
        }
    }
}