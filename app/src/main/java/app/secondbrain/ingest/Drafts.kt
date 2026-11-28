package app.secondbrain.ingest

import app.secondbrain.data.db.ItemEntity
import app.secondbrain.data.encodeTags
import app.secondbrain.tagging.ItemAction
import app.secondbrain.tagging.ItemType
import app.secondbrain.tagging.Tag
import app.secondbrain.tagging.TagEngine
import app.secondbrain.tagging.TagInput
import app.secondbrain.tagging.TagKind
import app.secondbrain.tagging.normalizeTag
import java.util.UUID

/** A post waiting for the user's confirmation. Nothing here is saved yet. */
data class Draft(
    val key: String,
    val type: ItemType,
    val body: String = "",
    val url: String? = null,
    val imagePath: String? = null,
    val ocrText: String? = null,
    val imageLabels: List<String> = emptyList(),
    val meta: LinkMetadata? = null,
    /** Everything suggested, plus anything the user typed in. */
    val tags: List<Tag> = emptyList(),
    /** Topics found by a sibling post, such as the text read from a photo. */
    val borrowed: List<Tag> = emptyList(),
    /** Names the user switched off; they will not be saved. */
    val removed: Set<String> = emptySet(),
    val action: ItemAction? = null,
    /** True once the user picked (or cleared) the action, so re-tagging leaves it alone. */
    val actionChosen: Boolean = false,
    val done: Boolean = false,
    val groupId: String? = null,
    val scanned: Boolean = false,
    /** The page or photo couldn't be read; try again after saving. */
    val needsAnalysis: Boolean = false,
    val createdAt: Long,
) {
    val keptTags: List<Tag> get() = tags.filter { it.name !in removed }

    val canSave: Boolean get() = type != ItemType.TEXT || body.isNotBlank()

    fun tagInput() = TagInput(
        type = type,
        text = body,
        url = url,
        pageTitle = meta?.title,
        pageDescription = meta?.description,
        pageKeywords = meta?.keywords,
        siteName = meta?.siteName,
        ocrText = ocrText,
        imageLabels = imageLabels,
        scanned = scanned,
        createdAt = createdAt,
    )

    /** Derives suggestions again, keeping the user's own tags and choices. */
    fun retagged(engine: TagEngine): Draft {
        val result = engine.tag(tagInput())
        val typed = tags.filter { it.why == TYPED }
        return copy(
            tags = (typed + result.tags + borrowed).distinctBy { it.name }.sortedBy { it.kind.ordinal },
            action = if (actionChosen) action else result.action,
        )
    }

    fun toggleTag(name: String): Draft = copy(removed = if (name in removed) removed - name else removed + name)

    fun addTags(names: List<String>): Draft {
        val clean = names.map(::normalizeTag).filter { it.isNotEmpty() }.distinct()
        val have = tags.map { it.name }.toSet()
        val added = clean.filter { it !in have }.map { Tag(it, TagKind.MINE, TYPED) }
        return copy(tags = (tags + added).sortedBy { it.kind.ordinal }, removed = removed - clean.toSet())
    }

    fun choose(action: ItemAction?): Draft = copy(action = action, actionChosen = true, done = done && action != null)

    fun toItem(now: Long) = ItemEntity(
        type = type,
        body = body.trim(),
        url = url,
        imagePath = imagePath,
        ocrText = ocrText,
        imageLabels = imageLabels.joinToString(",").ifEmpty { null },
        linkTitle = meta?.title,
        linkDescription = meta?.description,
        linkSiteName = meta?.siteName,
        linkKeywords = meta?.keywords,
        linkImageUrl = meta?.imageUrl,
        tagsJson = encodeTags(keptTags),
        removedTags = removed.joinToString(","),
        action = action,
        completedAt = now.takeIf { done && action != null },
        groupId = groupId,
        needsAnalysis = needsAnalysis,
        scanned = scanned,
        taggerVersion = TagEngine.VERSION,
        createdAt = createdAt,
        updatedAt = now,
    )

    companion object {
        const val TYPED = "Added by you"
    }
}

/** What was read from a capture, turned into the posts to review. Pure, so it is easy to test. */
object DraftAssembler {

    private const val MIN_LETTERS = 12
    private const val MIN_WORDS = 3

    fun assemble(
        text: String,
        imagePath: String?,
        analysis: ImageAnalysis?,
        meta: LinkMetadata?,
        engine: TagEngine,
        now: Long,
        newKey: () -> String = { UUID.randomUUID().toString() },
    ): List<Draft> {
        val written = text.trim()
        val drafts = if (imagePath != null) {
            val read = analysis?.text?.trim()?.takeIf { it.isNotEmpty() }
            // A photo with real text in it becomes two posts: the picture, and the words.
            val separate = read != null && isMeaningfulText(read)
            val group = if (separate) newKey() else null
            val photo = Draft(
                key = newKey(),
                type = ItemType.IMAGE,
                body = written,
                imagePath = imagePath,
                ocrText = read.takeUnless { separate },
                imageLabels = analysis?.labels.orEmpty(),
                groupId = group,
                needsAnalysis = analysis == null || analysis.failed,
                createdAt = now,
            )
            val words = if (separate) {
                Draft(key = newKey(), type = ItemType.TEXT, body = read!!, scanned = true, groupId = group, createdAt = now)
            } else null
            listOfNotNull(photo, words)
        } else {
            val parsed = LinkParser.parse(written)
            when {
                parsed.url != null -> listOf(
                    Draft(
                        key = newKey(), type = ItemType.LINK, body = parsed.note, url = parsed.url,
                        meta = meta, needsAnalysis = meta == null, createdAt = now,
                    ),
                )
                parsed.note.isNotBlank() -> listOf(Draft(key = newKey(), type = ItemType.TEXT, body = parsed.note, createdAt = now))
                else -> emptyList()
            }
        }
        val tagged = drafts.map { it.retagged(engine) }
        val words = tagged.firstOrNull { it.scanned } ?: return tagged
        val topics = words.tags.filter { it.kind == TagKind.MINE || it.kind == TagKind.TOPIC }
            .map { it.copy(why = "From the text in the photo") }
        return tagged.map { if (it.type == ItemType.IMAGE) it.copy(borrowed = topics).retagged(engine) else it }
    }

    /** More than a stray logo or price tag. */
    fun isMeaningfulText(text: String): Boolean {
        val words = text.split(Regex("\\s+")).count { word -> word.count { it.isLetter() } >= 2 }
        return words >= MIN_WORDS && text.count { it.isLetter() } >= MIN_LETTERS
    }
}
