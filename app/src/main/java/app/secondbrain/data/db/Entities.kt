package app.secondbrain.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.secondbrain.tagging.ItemAction
import app.secondbrain.tagging.ItemType

@Entity(tableName = "items", indices = [Index("createdAt")])
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ItemType,
    val title: String? = null,
    val body: String = "",
    val url: String? = null,
    val imagePath: String? = null,
    val ocrText: String? = null,
    /** Comma separated image labels. */
    val imageLabels: String? = null,
    val linkTitle: String? = null,
    val linkDescription: String? = null,
    val linkSiteName: String? = null,
    val linkKeywords: String? = null,
    val linkImageUrl: String? = null,
    /** JSON-encoded List<Tag>. */
    @ColumnInfo(defaultValue = "[]") val tagsJson: String = "[]",
    /** Comma separated tag names the user removed; automatic tagging never brings them back. */
    @ColumnInfo(defaultValue = "") val removedTags: String = "",
    /** Newline separated folder and tag names from the folder-based app, waiting to become tags. */
    @ColumnInfo(defaultValue = "") val legacyTags: String = "",
    /** Set when this is something to read, watch, buy or do. */
    val action: ItemAction? = null,
    val completedAt: Long? = null,
    /** Shared by posts that came from one capture, such as a photo and the text read from it. */
    val groupId: String? = null,
    /** True while a page or photo still has to be read. */
    @ColumnInfo(defaultValue = "0") val needsAnalysis: Boolean = false,
    /** True when [body] was read out of a photo. */
    @ColumnInfo(defaultValue = "0") val scanned: Boolean = false,
    /** The tagging version applied to this row; 0 means never tagged. */
    @ColumnInfo(defaultValue = "0") val taggerVersion: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)
