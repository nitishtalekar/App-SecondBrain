package app.secondbrain.data

import app.secondbrain.data.db.ItemEntity
import app.secondbrain.tagging.Tag
import app.secondbrain.tagging.TagEngine
import app.secondbrain.tagging.TagKind
import app.secondbrain.tagging.normalizeTag

/*
 * Pure rules for changing an item. The shared principle: what the user did by hand always beats
 * what automatic tagging would do.
 */

/** Adds newly derived tags, never touching existing ones and never reviving a removed one. */
fun ItemEntity.withAutoTags(fresh: List<Tag>): ItemEntity {
    val current = decodeTags(tagsJson)
    val have = current.map { it.name }.toSet()
    val removed = removedTagSet()
    val added = fresh.filter { it.name !in have && it.name !in removed }
    return copy(tagsJson = encodeTags((current + added).sortedBy { it.kind.ordinal }))
}

/** After the content changes, automatic tags are derived again and the user's own tags are kept. */
fun ItemEntity.retaggedForNewContent(fresh: List<Tag>): ItemEntity {
    val mine = decodeTags(tagsJson).filter { it.kind == TagKind.MINE }
    return copy(tagsJson = encodeTags(mine)).withAutoTags(fresh)
}

fun ItemEntity.withUserTags(names: List<String>): ItemEntity {
    val clean = names.map(::normalizeTag).filter { it.isNotEmpty() }.distinct()
    val current = decodeTags(tagsJson)
    val have = current.map { it.name }.toSet()
    val added = clean.filter { it !in have }.map { Tag(it, TagKind.MINE, "Added by you") }
    return copy(
        tagsJson = encodeTags((current + added).sortedBy { it.kind.ordinal }),
        removedTags = (removedTagSet() - clean.toSet()).joinToString(","),
    )
}

fun ItemEntity.withoutTag(name: String): ItemEntity = copy(
    tagsJson = encodeTags(decodeTags(tagsJson).filter { it.name != name }),
    removedTags = (removedTagSet() + name).joinToString(","),
)

/** Brings an item saved by the folder-based app into the tag world. Its folders become the user's tags. */
fun ItemEntity.migrated(engine: TagEngine, now: Long): ItemEntity {
    val result = engine.tag(tagInput())
    return withUserTags(legacyTags.split('\n'))
        .withAutoTags(result.tags)
        .copy(action = action ?: result.action, legacyTags = "", taggerVersion = TagEngine.VERSION, updatedAt = now)
}
