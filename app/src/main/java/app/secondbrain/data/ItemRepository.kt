package app.secondbrain.data

import android.net.Uri
import app.secondbrain.data.db.CategoryCount
import app.secondbrain.data.db.ItemDao
import app.secondbrain.data.db.ItemEntity
import app.secondbrain.data.db.ItemType
import app.secondbrain.data.db.ProcessingStatus
import app.secondbrain.ingest.ImageStore
import app.secondbrain.ingest.ItemProcessor
import app.secondbrain.ingest.LinkParser
import app.secondbrain.ingest.ProcessingScheduler
import app.secondbrain.ingest.classifyInput
import app.secondbrain.ingest.withClassification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Capture is always: persist first, analyse later. [saveText] and [saveImage] return as soon as the row
 * exists; categorisation happens in the background and can fail without losing anything.
 */
@Singleton
class ItemRepository @Inject constructor(
    private val dao: ItemDao,
    private val categories: CategoryRepository,
    private val images: ImageStore,
    private val scheduler: ProcessingScheduler,
    private val processor: ItemProcessor,
) {
    fun observeAll(): Flow<List<ItemEntity>> = dao.observeAll()
    fun observe(id: Long): Flow<ItemEntity?> = dao.observe(id)
    fun observeCategory(categoryId: String): Flow<List<ItemEntity>> = dao.observeByCategory(categoryId)
    fun observeInbox(): Flow<List<ItemEntity>> = dao.observeInbox()
    fun observeCounts(): Flow<List<CategoryCount>> = dao.observeCounts()

    fun search(query: String): Flow<List<ItemEntity>> {
        val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (terms.isEmpty()) return emptyFlow()
        val escaped = terms.first().replace("!", "!!").replace("%", "!%").replace("_", "!_")
        return kotlinx.coroutines.flow.flow {
            dao.search(escaped).collect { rows ->
                emit(rows.filter { row -> terms.drop(1).all { row.searchText.contains(it) } })
            }
        }
    }

    suspend fun saveText(raw: String, attachment: Uri? = null): Long? {
        val parsed = LinkParser.parse(raw)
        if (attachment == null && parsed.url == null && parsed.note.isBlank()) return null
        val now = System.currentTimeMillis()
        val imagePath = attachment?.let { images.import(it) }
        val type = when {
            imagePath != null -> ItemType.IMAGE
            parsed.url != null -> ItemType.LINK
            else -> ItemType.TEXT
        }
        val item = ItemEntity(
            type = type,
            body = parsed.note,
            url = parsed.url.takeIf { type == ItemType.LINK },
            imagePath = imagePath,
            status = ProcessingStatus.PENDING,
            createdAt = now,
            updatedAt = now,
        )
        val saved = item.copy(searchText = item.buildSearchText())
        val id = dao.insert(saved)
        scheduler.enqueue(id)
        return id
    }

    suspend fun updateContent(id: Long, title: String?, body: String, url: String?) {
        val current = dao.get(id) ?: return
        val cleanUrl = url?.trim()?.takeIf { it.isNotEmpty() }?.let(LinkParser::normalize)
        val changedUrl = current.type == ItemType.LINK && cleanUrl != current.url
        var next = current.copy(
            title = title?.trim()?.takeIf { it.isNotEmpty() },
            body = body.trim(),
            url = if (current.type == ItemType.LINK) cleanUrl ?: current.url else current.url,
            updatedAt = System.currentTimeMillis(),
        )
        next = next.copy(searchText = next.buildSearchText())
        if (changedUrl) {
            dao.update(next.copy(status = ProcessingStatus.PENDING))
            scheduler.enqueue(id)
        } else {
            dao.update(runCatching { next.withClassification(categories.classifier().classify(next.classifyInput())) }.getOrDefault(next))
        }
    }

    /** [categoryId] null means "put it in the Inbox". Either way the choice is locked in. */
    suspend fun setCategory(id: Long, categoryId: String?) {
        val current = dao.get(id) ?: return
        dao.update(current.copy(categoryId = categoryId, categoryLocked = true, updatedAt = System.currentTimeMillis()))
    }

    suspend fun unlockCategory(id: Long) {
        val current = dao.get(id) ?: return
        val unlocked = current.copy(categoryLocked = false)
        dao.update(runCatching { unlocked.withClassification(categories.classifier().classify(unlocked.classifyInput())) }.getOrDefault(unlocked))
    }

    suspend fun retry(id: Long) {
        val current = dao.get(id) ?: return
        dao.update(current.copy(status = ProcessingStatus.PENDING))
        scheduler.enqueue(id)
    }

    suspend fun delete(id: Long) {
        val current = dao.get(id) ?: return
        dao.delete(id)
        images.delete(current.imagePath)
    }

    suspend fun reclassifyAutoSorted() = processor.reclassifyAutoSorted()

    suspend fun resumeUnprocessed() = scheduler.resumeUnprocessed()
}
