package app.secondbrain.data

import app.secondbrain.data.db.ItemEntity
import app.secondbrain.tagging.ItemAction
import app.secondbrain.tagging.ItemType
import app.secondbrain.tagging.Tag
import app.secondbrain.tagging.TagInput
import app.secondbrain.tagging.Text
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val json = Json { ignoreUnknownKeys = true }
private val tagsSerializer = ListSerializer(Tag.serializer())

fun encodeTags(tags: List<Tag>): String = json.encodeToString(tagsSerializer, tags)

fun decodeTags(raw: String): List<Tag> =
    runCatching { json.decodeFromString(tagsSerializer, raw) }.getOrDefault(emptyList())

/** A saved item with its tags decoded once, ready for the screens, search and clustering. */
data class Entry(val item: ItemEntity, val tags: List<Tag>) {
    constructor(item: ItemEntity) : this(item, decodeTags(item.tagsJson))

    val id: Long get() = item.id
    val title: String get() = item.displayTitle()
    val done: Boolean get() = item.action != null && item.completedAt != null
    /** The action still waiting, if any. */
    val openAction: ItemAction? get() = item.action.takeIf { item.completedAt == null }
}

private fun String.firstLine(): String? = lineSequence().firstOrNull { it.isNotBlank() }?.trim()

fun ItemEntity.displayTitle(): String {
    title?.takeIf { it.isNotBlank() }?.let { return it }
    return when (type) {
        ItemType.TEXT -> body.firstLine() ?: "Untitled"
        ItemType.LINK -> linkTitle?.takeIf { it.isNotBlank() }
            ?: body.firstLine()
            ?: url?.let { Text.hostOf(it) ?: it }
            ?: "Link"
        ItemType.IMAGE -> body.firstLine()
            ?: ocrText?.firstLine()?.take(80)
            ?: "Photo"
    }
}

fun ItemEntity.labelList(): List<String> =
    imageLabels?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

fun ItemEntity.removedTagSet(): Set<String> =
    removedTags.split(',').filter { it.isNotEmpty() }.toSet()

fun ItemEntity.tagInput() = TagInput(
    type = type,
    text = listOfNotNull(title, body).joinToString("\n").trim(),
    url = url,
    pageTitle = linkTitle,
    pageDescription = linkDescription,
    pageKeywords = linkKeywords,
    siteName = linkSiteName,
    ocrText = ocrText,
    imageLabels = labelList(),
    scanned = scanned,
    createdAt = createdAt,
)
