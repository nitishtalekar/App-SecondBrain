package app.secondbrain.data

import app.secondbrain.classify.Classifier
import app.secondbrain.data.db.ItemEntity
import app.secondbrain.data.db.ItemType
import kotlinx.serialization.Serializable

@Serializable
data class Suggestion(val categoryId: String, val score: Double)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private fun String.firstLine(): String? = lineSequence().firstOrNull { it.isNotBlank() }?.trim()

fun ItemEntity.displayTitle(): String {
    title?.takeIf { it.isNotBlank() }?.let { return it }
    return when (type) {
        ItemType.TEXT -> body.firstLine() ?: "Untitled"
        ItemType.LINK -> linkTitle?.takeIf { it.isNotBlank() }
            ?: body.firstLine()
            ?: url?.let { Classifier.hostOf(it) ?: it }
            ?: "Link"
        ItemType.IMAGE -> body.firstLine() ?: ocrText?.firstLine()?.take(80) ?: "Photo"
    }
}

fun ItemEntity.displaySubtitle(): String? = when (type) {
    ItemType.TEXT -> body.lines().filter { it.isNotBlank() }
        .drop(if (title.isNullOrBlank()) 1 else 0)
        .joinToString(" ").trim().ifBlank { null }
    ItemType.LINK -> linkDescription?.takeIf { it.isNotBlank() } ?: url?.let { Classifier.hostOf(it) }
    ItemType.IMAGE -> {
        val ocr = ocrText?.lines()?.filter { it.isNotBlank() }.orEmpty()
        val remaining = if (title.isNullOrBlank() && body.isBlank()) ocr.drop(1) else ocr
        remaining.joinToString(" ").trim().ifBlank { null }
            ?: labelList().take(3).joinToString(" · ").ifBlank { null }
    }
}

fun ItemEntity.labelList(): List<String> =
    imageLabels?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

fun ItemEntity.tagList(): List<String> =
    tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }

fun normalizeTag(raw: String): String =
    raw.trim().trimStart('#').lowercase().replace(Regex("[,\\s]+"), "-").trim('-')

fun ItemEntity.buildSearchText(): String = listOfNotNull(
    title, body, url, ocrText, imageLabels, linkTitle, linkDescription, linkSiteName, linkKeywords, tags.replace(',', ' '),
).joinToString("\n").lowercase()
