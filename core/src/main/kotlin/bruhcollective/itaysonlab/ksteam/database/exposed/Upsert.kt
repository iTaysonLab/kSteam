package bruhcollective.itaysonlab.ksteam.database.exposed

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.vendors.H2Dialect
import org.jetbrains.exposed.sql.vendors.currentDialect

/**
 * Batch upsert (Update/Insert) implementation based on Exposed GitHub issues, but:
 * - adapted to H2
 * - slightly rewritten
 */
fun <T : Table, E> T.batchUpsert(
    data: Collection<E>,
    body: BatchInsertStatement.(E) -> Unit
) =
    BatchInsertOrUpdate(this).apply {
        data.forEach {
            addBatch()
            body(it)
        }

        execute(TransactionManager.current())
    }

class BatchInsertOrUpdate(
    table: Table,
    isIgnore: Boolean = false,
) : BatchInsertStatement(table, isIgnore, shouldReturnGeneratedValues = false) {
    override fun prepareSQL(transaction: Transaction): String {
        require(currentDialect is H2Dialect) { "This upsert implementation is made only for H2 databases!" }

        QueryBuilder(true).apply {
            append("MERGE INTO ${transaction.identity(table)}")
            arguments!!.first().appendTo(prefix = " (", postfix = ") ") { (col, _) -> append(transaction.identity(col)) }
            append("KEY(${transaction.identity((table as? IdTable<*>)?.id ?: error("Table should be IdTable!"))})")
            arguments!!.first().appendTo(prefix = " VALUES (", postfix = ")") { (col, value) -> append(value.toString()) }
        }.toString().also { println(it) }

        return QueryBuilder(true).apply {
            append("MERGE INTO ${transaction.identity(table)}")
            arguments!!.first().appendTo(prefix = " (", postfix = ") ") { (col, _) -> append(transaction.identity(col)) }
            append("KEY(${transaction.identity((table as? IdTable<*>)?.id ?: error("Table should be IdTable!"))})")
            arguments!!.first().appendTo(prefix = " VALUES (", postfix = ")") { (col, value) -> registerArgument(col, value) }
        }.toString()
    }
}