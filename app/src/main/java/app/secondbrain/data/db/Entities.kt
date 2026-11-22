package app.secondbrain.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ItemType { TEXT, LINK, IMAGE }

enum class ProcessingStatus { PENDING, DONE, FAILED }

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String,
    val sortOrder: Int,
    /** Parent folder id; null for top-level folders. */
    val parentId: String? = null,
    val minScore: Double? = null,
    /** JSON-encoded List<Rule>. Kept opaque so rule shapes can evolve without migrations. */
    val rulesJson: String = "[]",
)

@Entity(
    tableName = "items",
    indices = [Index("categoryId"), Index("createdAt")],
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ItemType,
    val title: String? = null,
    val body: String = "",
    val url: String? = null,
    val imagePath: String? = null,
    val ocrText: String? = null,
    /** Comma separated ML Kit labels. */
    val imageLabels: String? = null,
    val linkTitle: String? = null,
    val linkDescription: String? = null,
    val linkSiteName: String? = null,
    val linkKeywords: String? = null,
    val linkImageUrl: String? = null,
    /** Null means Inbox (or still processing, see [status]). */
    val categoryId: String? = null,
    /** True once the user chose a category by hand; automatic sorting never touches it again. */
    val categoryLocked: Boolean = false,
    val confidence: Double = 0.0,
    /** JSON-encoded List<Suggestion>, best first. */
    val suggestionsJson: String = "[]",
    val reasons: String = "",
    /** Comma separated, lowercase, user-added tags. */
    @ColumnInfo(defaultValue = "") val tags: String = "",
    val status: ProcessingStatus = ProcessingStatus.PENDING,
    val createdAt: Long,
    val updatedAt: Long,
    /** Lowercased concatenation of every searchable field. */
    @ColumnInfo(defaultValue = "") val searchText: String = "",
)

data class CategoryCount(val categoryId: String?, val count: Int)
