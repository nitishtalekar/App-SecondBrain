package app.secondbrain.ingest

import app.secondbrain.data.TagRulesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads whatever was captured (photo text and labels, page details) and drafts the posts. Every read
 * is best-effort: a failure means fewer tags, never a lost capture.
 */
@Singleton
class DraftBuilder @Inject constructor(
    private val analyzer: ImageAnalyzer,
    private val links: LinkMetadataFetcher,
    private val rules: TagRulesRepository,
) {
    suspend fun build(text: String, imagePath: String?): List<Draft> = withContext(Dispatchers.Default) {
        val analysis = imagePath?.let { path -> attempt { analyzer.analyze(path) } }
        val url = if (imagePath == null) LinkParser.parse(text).url else null
        val meta = url?.let { attempt { links.fetch(it) } }
        DraftAssembler.assemble(text, imagePath, analysis, meta, rules.engine(), System.currentTimeMillis())
    }

    private inline fun <T> attempt(block: () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}
