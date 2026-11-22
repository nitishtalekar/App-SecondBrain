package app.secondbrain.ingest

import app.secondbrain.classify.Classification
import app.secondbrain.classify.ClassifyInput
import app.secondbrain.classify.Classifier
import app.secondbrain.data.CategoryRepository
import app.secondbrain.data.Suggestion
import app.secondbrain.data.buildSearchText
import app.secondbrain.data.labelList
import app.secondbrain.data.db.ItemDao
import app.secondbrain.data.db.ItemEntity
import app.secondbrain.data.db.ItemType
import app.secondbrain.data.db.ProcessingStatus
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val json = Json { ignoreUnknownKeys = true }
private val suggestionsSerializer = ListSerializer(Suggestion.serializer())

fun ItemEntity.classifyInput() = ClassifyInput(
    text = listOfNotNull(title, body).joinToString("\n"),
    url = url,
    pageTitle = linkTitle,
    pageDescription = linkDescription,
    pageKeywords = listOfNotNull(linkSiteName, linkKeywords).joinToString(", ").ifEmpty { null },
    ocrText = ocrText,
    imageLabels = labelList(),
)

/** Applies a classification without ever overriding a category the user chose by hand. */
fun ItemEntity.withClassification(result: Classification): ItemEntity = copy(
    categoryId = if (categoryLocked) categoryId else result.categoryId,
    confidence = if (categoryLocked) confidence else result.confidence,
    suggestionsJson = json.encodeToString(
        suggestionsSerializer,
        result.ranked.take(MAX_SUGGESTIONS).map { Suggestion(it.categoryId, it.score) },
    ),
    reasons = if (categoryLocked) "" else result.reasons.joinToString(", "),
    searchText = buildSearchText(),
)

fun ItemEntity.suggestions(): List<Suggestion> =
    runCatching { json.decodeFromString(suggestionsSerializer, suggestionsJson) }.getOrDefault(emptyList())

private const val MAX_SUGGESTIONS = 5

/**
 * Analyse -> classify -> save. Each analysis step is isolated: whatever it yields is kept, and the
 * item is classified from whatever evidence exists. The row is already in the database before this
 * runs, so no failure here can lose a capture.
 */
@Singleton
class ItemProcessor @Inject constructor(
    private val dao: ItemDao,
    private val categories: CategoryRepository,
    private val imageAnalyzer: ImageAnalyzer,
    private val linkFetcher: LinkMetadataFetcher,
) {
    suspend fun process(id: Long) {
        var item = dao.get(id) ?: return
        var failed = false

        if (item.type == ItemType.IMAGE && item.imagePath != null) {
            try {
                val analysis = imageAnalyzer.analyze(item.imagePath!!)
                failed = analysis.failed
                item = item.copy(ocrText = analysis.text ?: item.ocrText, imageLabels = analysis.labels.joinToString(",").ifEmpty { null })
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                failed = true
            }
        }

        if (item.type == ItemType.LINK && item.url != null) {
            try {
                val meta = linkFetcher.fetch(item.url!!)
                item = item.copy(
                    linkTitle = meta.title, linkDescription = meta.description, linkSiteName = meta.siteName,
                    linkKeywords = meta.keywords, linkImageUrl = meta.imageUrl,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                failed = true
            }
        }

        val result = runCatching { categories.classifier().classify(item.classifyInput()) }.getOrNull()
        item = (if (result != null) item.withClassification(result) else item.copy(searchText = item.buildSearchText()))
        dao.update(item.copy(status = if (failed) ProcessingStatus.FAILED else ProcessingStatus.DONE, updatedAt = System.currentTimeMillis()))
    }

    /** Re-run classification only (no OCR / network) for every item the user hasn't placed manually. */
    suspend fun reclassifyAutoSorted() {
        val classifier = categories.classifier()
        for (item in dao.getAutoSorted()) {
            dao.update(item.withClassification(classifier.classify(item.classifyInput())))
        }
    }
}
