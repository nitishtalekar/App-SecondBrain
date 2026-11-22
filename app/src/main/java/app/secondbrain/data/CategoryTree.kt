package app.secondbrain.data

import app.secondbrain.data.db.CategoryEntity

/** A folder with its depth, in display order (parents before their children). */
data class TreeNode(val category: CategoryEntity, val depth: Int)

fun List<CategoryEntity>.children(parentId: String?): List<CategoryEntity> = filter { it.parentId == parentId }

/** Depth-first flattening so a picker can show the hierarchy as indented rows. */
fun List<CategoryEntity>.flattened(): List<TreeNode> {
    val byParent = groupBy { it.parentId }
    val out = mutableListOf<TreeNode>()
    fun walk(parent: String?, depth: Int, seen: Set<String>) {
        byParent[parent].orEmpty().forEach { c ->
            if (c.id in seen) return@forEach
            out += TreeNode(c, depth)
            walk(c.id, depth + 1, seen + c.id)
        }
    }
    walk(null, 0, emptySet())
    // Orphans (parent missing) show at the top level rather than disappearing.
    val ids = map { it.id }.toSet()
    forEach { c ->
        if (c.parentId != null && c.parentId !in ids && out.none { it.category.id == c.id }) {
            out += TreeNode(c, 0)
            walk(c.id, 1, setOf(c.id))
        }
    }
    return out
}

/** Ancestors from the top level down to (and including) [id]. */
fun List<CategoryEntity>.pathTo(id: String?): List<CategoryEntity> {
    val byId = associateBy { it.id }
    val path = ArrayDeque<CategoryEntity>()
    var cursor = id?.let(byId::get)
    while (cursor != null && path.none { it.id == cursor?.id }) {
        path.addFirst(cursor)
        cursor = cursor.parentId?.let(byId::get)
    }
    return path
}

fun List<CategoryEntity>.pathName(id: String?): String? =
    pathTo(id).takeIf { it.isNotEmpty() }?.joinToString(" › ") { it.name }

/** Item count for each folder including everything filed under its subfolders. */
fun List<CategoryEntity>.recursiveCounts(direct: Map<String?, Int>): Map<String, Int> {
    val byParent = groupBy { it.parentId }
    fun total(id: String, seen: Set<String>): Int =
        (direct[id] ?: 0) + byParent[id].orEmpty().filter { it.id !in seen }.sumOf { total(it.id, seen + it.id) }
    return associate { it.id to total(it.id, setOf(it.id)) }
}
