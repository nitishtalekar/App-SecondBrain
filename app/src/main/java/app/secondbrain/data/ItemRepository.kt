package app.secondbrain.data

import app.secondbrain.data.db.ItemDao
import app.secondbrain.data.db.ItemEntity
import app.secondbrain.ingest.Draft
import app.secondbrain.ingest.ImageAnalyzer
import app.secondbrain.ingest.ImageStore
import app.secondbrain.ingest.LinkMetadataFetcher
import app.secondbrain.ingest.LinkParser
import app.secondbrain.tagging.ItemAction
import app.secondbrain.tagging.ItemType
import app.secondbrain.tagging.TagEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val dao: ItemDao,
    private val rules: TagRulesRepository,
    private val images: ImageStore,
    private val analyzer: ImageAnalyzer,
    private val links: LinkMetadataFetcher,
) {
    /** Serialises read-modify-write so background work never overwrites a tap. */
    private val writes = Mutex()

    val entries: Flow<List<Entry>> = dao.observeAll()
        .map { rows -> rows.map { Entry(it) } }
        .flowOn(Dispatchers.Default)

    /** Saves the posts the user confirmed. */
    suspend fun add(drafts: List<Draft>): List<Long> =
        dao.insertAll(drafts.map { it.toItem(System.currentTimeMillis()) })

    suspend fun setCompleted(id: Long, completed: Boolean) = change(id) {
        it.copy(completedAt = if (completed) System.currentTimeMillis() else null)
    }

    suspend fun setAction(id: Long, action: ItemAction?) = change(id) {
        if (it.action == action) it else it.copy(action = action, completedAt = null)
    }

    suspend fun addTags(id: Long, names: List<String>) = change(id) { it.withUserTags(names) }

    suspend fun removeTag(id: Long, name: String) = change(id) { it.withoutTag(name) }

    suspend fun edit(id: Long, title: String?, body: String, url: String?) {
        val engine = rules.engine()
        var refetch = false
        change(id) { current ->
            val cleanUrl = url?.trim()?.takeIf { it.isNotEmpty() }?.let(LinkParser::normalize)
            refetch = current.type == ItemType.LINK && cleanUrl != null && cleanUrl != current.url
            var next = current.copy(title = title?.trim()?.takeIf { it.isNotEmpty() }, body = body.trim())
            if (refetch) {
                // The old page's details no longer describe this link.
                next = next.copy(
                    url = cleanUrl, linkTitle = null, linkDescription = null, linkSiteName = null,
                    linkKeywords = null, linkImageUrl = null, needsAnalysis = true,
                )
            }
            next.retaggedForNewContent(engine.tag(next.tagInput()).tags)
        }
        if (refetch) analyse(id)
    }

    suspend fun delete(id: Long) = withContext(NonCancellable) {
        writes.withLock {
            val current = dao.get(id) ?: return@withLock
            dao.delete(id)
            current.imagePath?.let { if (dao.countImageUses(it) == 0) images.delete(it) }
        }
    }

    /** Reads a page or photo that couldn't be read before, and adds whatever tags it yields. */
    suspend fun analyse(id: Long) {
        val engine = rules.engine()
        val start = dao.get(id) ?: return
        try {
            if (start.type == ItemType.LINK && start.url != null) {
                val meta = links.fetch(start.url)
                change(id) { current ->
                    if (current.url != start.url) return@change current
                    val next = current.copy(
                        linkTitle = meta.title, linkDescription = meta.description, linkSiteName = meta.siteName,
                        linkKeywords = meta.keywords, linkImageUrl = meta.imageUrl, needsAnalysis = false,
                    )
                    next.withAutoTags(engine.tag(next.tagInput()).tags)
                }
            } else if (start.type == ItemType.IMAGE && start.imagePath != null) {
                val analysis = analyzer.analyze(start.imagePath)
                change(id) { current ->
                    val next = current.copy(
                        ocrText = analysis.text ?: current.ocrText,
                        imageLabels = analysis.labels.joinToString(",").ifEmpty { current.imageLabels },
                        needsAnalysis = analysis.failed,
                    )
                    next.withAutoTags(engine.tag(next.tagInput()).tags)
                }
            } else {
                change(id) { it.copy(needsAnalysis = false) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Still offline or unreadable. It stays flagged and is retried on a later launch.
        }
    }

    /** Startup housekeeping: tag anything from the folder era, then retry unread pages and photos. */
    suspend fun catchUp() = withContext(Dispatchers.Default) {
        val engine = rules.engine()
        dao.getTaggedBefore(TagEngine.VERSION).forEach { id ->
            change(id) { it.migrated(engine, System.currentTimeMillis()) }
        }
        dao.getNeedingAnalysis().forEach { analyse(it.id) }
    }

    /** Applies current rules to everything, only ever adding tags. Returns how many items gained some. */
    suspend fun retagAll(): Int = withContext(Dispatchers.Default) {
        val engine = rules.engine()
        var changed = 0
        dao.getAll().forEach { item ->
            change(item.id) { current ->
                current.withAutoTags(engine.tag(current.tagInput()).tags)
                    .also { if (it.tagsJson != current.tagsJson) changed++ }
            }
        }
        changed
    }

    private suspend fun change(id: Long, transform: (ItemEntity) -> ItemEntity) {
        writes.withLock {
            val current = dao.get(id) ?: return
            val next = transform(current)
            if (next != current) dao.update(next.copy(updatedAt = System.currentTimeMillis()))
        }
    }
}
