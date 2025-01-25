package bruhcollective.itaysonlab.ksteam.database.sql

import androidx.room.RoomRawQuery
import bruhcollective.itaysonlab.ksteam.models.enums.ESteamDeckSupport
import bruhcollective.itaysonlab.ksteam.models.enums.EStoreCategory
import bruhcollective.itaysonlab.ksteam.models.library.query.KsLibraryQuery
import bruhcollective.itaysonlab.ksteam.models.library.query.KsLibraryQueryControllerSupportFilter
import bruhcollective.itaysonlab.ksteam.models.library.query.KsLibraryQuerySortBy
import bruhcollective.itaysonlab.ksteam.models.library.query.KsLibraryQuerySortByDirection

/**
 * Builds a really, really large SQL query for [bruhcollective.itaysonlab.ksteam.database.room.entity.pics.apps.RoomPicsAppInfo]
 */
internal fun compileKsLibraryQueryToSql(query: KsLibraryQuery): RoomRawQuery {
    val binds = mutableListOf<SqlBindValue>()

    val sql = buildString {
        // == VIEWS
        append("WITH ")
        appendViewAppQuery(query, binds::add)
        append(',')

        val shouldJoinWithCategories: Boolean = appendViewCategoryQuery(query)
        append(',')

        var shouldJoinWithTags: Boolean = appendViewTagsQuery(query)

        if (!shouldJoinWithTags) {
            if (!shouldJoinWithCategories) {
                deleteRange(lastIndex - 1, lastIndex + 1)
            } else {
                deleteRange(lastIndex, lastIndex + 1)
            }
        }

        appendLine()

        // == SELECTION
        appendLine("SELECT app.* FROM app_info app")
        if (shouldJoinWithCategories) appendLine("INNER JOIN ${SqlConstants.VIEW_CATEGORIES} c ON c.app_id = app.id")
        if (shouldJoinWithTags) appendLine("INNER JOIN ${SqlConstants.VIEW_TAGS} t ON c.app_id = app.id")

        if (shouldJoinWithCategories || shouldJoinWithTags) {
            append("WHERE ")

            if (shouldJoinWithCategories) append("c.cat_cnt != 0")
            if (shouldJoinWithCategories && shouldJoinWithTags) append(" AND ")
            if (shouldJoinWithTags) append("t.tag_cnt = ").append(query.storeTags.size)

            appendLine()
        }

        appendLine("GROUP BY app.id")

        when (query.sortBy) {
            KsLibraryQuerySortBy.None, KsLibraryQuerySortBy.PlayedTime, KsLibraryQuerySortBy.LastPlayed -> Unit
            KsLibraryQuerySortBy.AppId -> append("ORDER BY app.id")
            KsLibraryQuerySortBy.Name -> append("ORDER BY app.name")
            KsLibraryQuerySortBy.ReleaseDate -> append("ORDER BY app.steam_release_date")
            KsLibraryQuerySortBy.MetacriticScore -> append("ORDER BY app.metacritic_score")
            KsLibraryQuerySortBy.SteamScore -> append("ORDER BY app.review_score")
        }

        if (query.sortBy != KsLibraryQuerySortBy.None && query.sortBy != KsLibraryQuerySortBy.PlayedTime && query.sortBy != KsLibraryQuerySortBy.LastPlayed) {
            when (query.sortByDirection) {
                KsLibraryQuerySortByDirection.Ascending -> appendLine(" ASC")
                KsLibraryQuerySortByDirection.Descending -> appendLine(" DESC")
            }
        } else {
            appendLine()
        }

        if (query.limit > 0) {
            append("LIMIT ").appendLine(query.limit)
        }

        if (query.offset > 0) {
            append("OFFSET ").appendLine(query.offset)
        }
    }

    return RoomRawQuery(sql = sql) { statement ->
        binds.forEachIndexed { index, bind ->
            when (bind) {
                is SqlBindValue.Integer -> statement.bindInt(index + 1, bind.value)
                is SqlBindValue.String -> statement.bindText(index + 1, bind.value)
            }
        }
    }
}

private object SqlConstants {
    const val LINE_END_CHARACTER = ' '

    const val VIEW_APPS = "apps"
    const val VIEW_CATEGORIES = "cats"
    const val VIEW_TAGS = "tags"

    const val COLUMN_COUNT_CATEGORIES = "cat_cnt"
    const val COLUMN_COUNT_TAGS = "tag_cnt"
}

private sealed interface SqlBindValue {
    data class Integer (
        val value: Int
    ): SqlBindValue

    data class String (
        val value: kotlin.String
    ): SqlBindValue
}

// Appends a VIEW of apps
private fun StringBuilder.appendViewAppQuery(query: KsLibraryQuery, addBind: (SqlBindValue) -> Unit) {
    appendLine("${SqlConstants.VIEW_APPS} as (")
    append("SELECT id FROM app_info WHERE ")

    if (query.searchQuery != null) {
        // TODO: Localized?
        append("name LIKE '%' || ? || '%'")
        addBind(SqlBindValue.String(query.searchQuery))
        append(" AND ")
    }

    if (query.appType.isNotEmpty()) {
        if (query.appType.size == 1) {
            append("type = ?")
            addBind(SqlBindValue.String(query.appType.first().vdfName))
        } else {
            append("type IN (")

            for (type in query.appType) {
                append('?').append(',')
                addBind(SqlBindValue.String(type.vdfName))
            }

            deleteRange(lastIndex, lastIndex + 1)
            append(')')
        }

        append(" AND ")
    }

    if (query.masterSubPackageId != 0) {
        append("master_sub = ").append(query.masterSubPackageId)
        append(" AND ")
    }

    if (query.steamDeckMinimumSupport != ESteamDeckSupport.Unknown) {
        append("deck_compat >= ").append(query.steamDeckMinimumSupport.ordinal)
        append(" AND ")
    }

    when (query.controllerSupport) {
        KsLibraryQueryControllerSupportFilter.None -> Unit

        // Also, filter with ANY categories 18 or 28
        KsLibraryQueryControllerSupportFilter.Partial -> {
            append("(controller == 'full' OR controller == 'partial')")
        }

        // Also, filter with category 28
        KsLibraryQueryControllerSupportFilter.Full -> {
            append("controller == 'full'")
        }
    }

    if (this[lastIndex - 1] == 'E') {
        // (SELECT ... FROM ... WHERE )
        deleteRange(startIndex = lastIndex - 6, endIndex = lastIndex)
    } else if (this[lastIndex - 1] == 'D') {
        // (SELECT ... FROM ... WHERE ... AND )
        deleteRange(startIndex = lastIndex - 4, endIndex = lastIndex)
    }

    append(')')
}

// Appends a VIEW of categories
private fun StringBuilder.appendViewCategoryQuery(query: KsLibraryQuery): Boolean {
    if (query.storeCategories.isEmpty() && query.controllerSupport == KsLibraryQueryControllerSupportFilter.None) {
        return false // there is no filtering based on categories
    }

    val hasControllerCategories = query.controllerSupport != KsLibraryQueryControllerSupportFilter.None

    appendLine("${SqlConstants.VIEW_CATEGORIES} as (")
    appendLine("SELECT app_id, category_id, count(*) AS ${SqlConstants.COLUMN_COUNT_CATEGORIES} FROM app_info_categories aic")
    appendLine("INNER JOIN ${SqlConstants.VIEW_APPS} ON ${SqlConstants.VIEW_APPS}.id = aic.app_id")
    append("WHERE ")

    fun StringBuilder.appendESC(categories: List<EStoreCategory>) {
        append("app_id IN (SELECT app_id FROM app_info_categories WHERE category_id IN (")

        categories.forEachIndexed { idx, category ->
            append(category.ordinal)
            if (idx != categories.lastIndex) append(',')
        }

        append(')').appendLine(')')
    }

    if (query.controllerSupport == KsLibraryQueryControllerSupportFilter.Partial) {
        appendESC(listOf(EStoreCategory.PartialController, EStoreCategory.FullController))
    } else if (query.controllerSupport == KsLibraryQueryControllerSupportFilter.Full) {
        appendESC(listOf(EStoreCategory.FullController))
    }

    query.storeCategories.filter(List<EStoreCategory>::isNotEmpty).forEachIndexed { index, categories ->
        if (hasControllerCategories || index != 0) append("AND ")
        appendESC(categories)
    }

    appendLine("GROUP BY aic.app_id")
    append(')')
    return true
}

// Appends a VIEW of tags
private fun StringBuilder.appendViewTagsQuery(query: KsLibraryQuery): Boolean {
    if (query.storeTags.isEmpty()) return false // there is no filtering based on tags

    appendLine("${SqlConstants.VIEW_TAGS} as (")
    appendLine("SELECT app_id, tag_id, count(*) AS ${SqlConstants.COLUMN_COUNT_TAGS} FROM app_info_tags ait")
    appendLine("INNER JOIN ${SqlConstants.VIEW_APPS} ON ${SqlConstants.VIEW_APPS}.id = ait.app_id")

    append("WHERE ait.tag_id IN (")
    query.storeTags.forEachIndexed { index, tagId ->
        append(tagId)
        if (index != query.storeTags.lastIndex) append(',')
    }
    appendLine(')')

    appendLine("GROUP BY ait.app_id")
    append(')')
    return true
}