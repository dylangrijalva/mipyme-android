package co.mipyme.database.internal

import androidx.sqlite.db.SimpleSQLiteQuery
import co.mipyme.database.CatalogGrouping
import co.mipyme.database.CatalogQuery

internal fun CatalogQuery.toSqlQuery(count: Boolean = false): SimpleSQLiteQuery {
    val arguments = mutableListOf<Any>()
    val predicates = mutableListOf<String>()
    if (text.isNotEmpty()) {
        val pattern = text.toLiteralCaseInsensitiveGlob()
        predicates += """(p.name GLOB ? OR EXISTS (
            SELECT 1 FROM product_variants v WHERE v.product_id = p.id AND
            (v.name GLOB ? OR v.sku GLOB ? OR v.barcode GLOB ?)))
        """.trimIndent()
        repeat(4) { arguments += pattern }
    }
    if (categoryIds.isNotEmpty() || includeUncategorized) {
        val categories = mutableListOf<String>()
        if (categoryIds.isNotEmpty()) {
            categories += "p.collection_id IN (${categoryIds.joinToString { "?" }})"
            arguments.addAll(categoryIds.sorted())
        }
        if (includeUncategorized) categories += "p.collection_id IS NULL"
        predicates += "(${categories.joinToString(" OR ")})"
    }
    type?.let {
        predicates += "p.product_type = ?"
        arguments += it.name
    }
    if (statuses.isNotEmpty()) {
        predicates += "p.status IN (${statuses.joinToString { "?" }})"
        arguments.addAll(statuses.sortedBy { it.ordinal }.map { it.name })
    }
    // Read the current category name instead of the denormalized name on the product.
    val columns = if (count) "COUNT(*)" else """
        p.id, p.name, p.slug, p.unit_cost, p.price, p.product_type, p.status, p.image_url,
        p.collection_id, COALESCE(c.name, p.collection_name) AS collection_name,
        p.created_at, p.updated_at, p.archived_at
    """.trimIndent()
    val where = if (predicates.isEmpty()) "" else " WHERE ${predicates.joinToString(" AND ")}"
    val groupOrder = when (grouping) {
        CatalogGrouping.NONE -> ""
        CatalogGrouping.CATEGORY -> "p.collection_id IS NULL, COALESCE(c.name, p.collection_name) COLLATE NOCASE, p.collection_id, "
        CatalogGrouping.STATUS -> "CASE p.status WHEN 'ACTIVE' THEN 0 WHEN 'DRAFT' THEN 1 ELSE 2 END, "
    }
    val order = if (count) "" else " ORDER BY ${groupOrder}p.name COLLATE NOCASE, p.id"
    return SimpleSQLiteQuery(
        "SELECT $columns FROM products p LEFT JOIN collections c ON c.id = p.collection_id$where$order",
        arguments.toTypedArray(),
    )
}

/** SQLite LIKE only folds ASCII. GLOB character alternatives also cover Unicode simple case pairs. */
private fun String.toLiteralCaseInsensitiveGlob(): String {
    return buildString {
        append('*')
        this@toLiteralCaseInsensitiveGlob.codePoints().forEach { codePoint ->
            val folded = Character.toLowerCase(Character.toUpperCase(codePoint))
            val alternatives = unicodeCaseVariants[folded] ?: setOf(codePoint)
            if (alternatives.size > 1) {
                append('[')
                alternatives.forEach { appendCodePoint(it) }
                append(']')
            } else {
                when (codePoint) {
                    '*'.code -> append("[*]")
                    '?'.code -> append("[?]")
                    '['.code -> append("[[]")
                    ']'.code -> append("]")
                    else -> appendCodePoint(codePoint)
                }
            }
        }
        append('*')
    }
}

// Include reverse mappings too: i/I/İ/ı and σ/Σ/ς belong to the same simple-case class.
private val unicodeCaseVariants: Map<Int, Set<Int>> by lazy {
    val groups = mutableMapOf<Int, MutableSet<Int>>()
    for (codePoint in 0..Character.MAX_CODE_POINT) {
        val upper = Character.toUpperCase(codePoint)
        val folded = Character.toLowerCase(upper)
        if (upper != codePoint || folded != codePoint) {
            groups.getOrPut(folded) { mutableSetOf(folded) }.add(codePoint)
        }
    }
    groups
}
