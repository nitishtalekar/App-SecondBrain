package app.secondbrain

import app.secondbrain.data.Entry
import app.secondbrain.data.db.ItemEntity
import app.secondbrain.data.encodeTags
import app.secondbrain.tagging.ItemAction
import app.secondbrain.tagging.ItemType
import app.secondbrain.tagging.Tag
import app.secondbrain.tagging.TagEngine
import app.secondbrain.tagging.TagKind
import app.secondbrain.tagging.TagRules
import java.io.File

val defaultRules: TagRules by lazy { TagRules.parse(File("src/main/assets/tags.json").readText(Charsets.UTF_8)) }
val defaultEngine: TagEngine by lazy { TagEngine(defaultRules) }

fun entry(
    id: Long,
    body: String = "",
    tags: List<String> = emptyList(),
    kind: TagKind = TagKind.TOPIC,
    type: ItemType = ItemType.TEXT,
    action: ItemAction? = null,
    completedAt: Long? = null,
    createdAt: Long = id,
    ocrText: String? = null,
) = Entry(
    ItemEntity(
        id = id, type = type, body = body, ocrText = ocrText, action = action, completedAt = completedAt,
        tagsJson = encodeTags(tags.map { Tag(it, kind) }), createdAt = createdAt, updatedAt = createdAt,
    ),
)
