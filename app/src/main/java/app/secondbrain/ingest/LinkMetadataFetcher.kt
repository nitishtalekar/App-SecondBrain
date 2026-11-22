package app.secondbrain.ingest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

data class LinkMetadata(
    val title: String?,
    val description: String?,
    val siteName: String?,
    val keywords: String?,
    val imageUrl: String?,
)

/** Best-effort page metadata (title, Open Graph, keywords). Any failure just means less evidence. */
@Singleton
class LinkMetadataFetcher @Inject constructor() {

    suspend fun fetch(url: String): LinkMetadata = withContext(Dispatchers.IO) {
        val doc = Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .timeout(TIMEOUT_MS)
            .maxBodySize(MAX_BYTES)
            .followRedirects(true)
            .get()

        fun meta(vararg names: String): String? = names.firstNotNullOfOrNull { name ->
            doc.selectFirst("meta[property=$name], meta[name=$name]")?.attr("content")?.trim()?.takeIf { it.isNotEmpty() }
        }

        LinkMetadata(
            title = meta("og:title", "twitter:title") ?: doc.title().trim().takeIf { it.isNotEmpty() },
            description = meta("og:description", "description", "twitter:description"),
            siteName = meta("og:site_name"),
            keywords = listOfNotNull(meta("keywords", "article:tag"), meta("og:type")).joinToString(", ").takeIf { it.isNotEmpty() },
            imageUrl = meta("og:image", "twitter:image")?.let { resolve(doc.location(), it) },
        )
    }

    private fun resolve(base: String, href: String): String =
        runCatching { java.net.URL(java.net.URL(base), href).toString() }.getOrDefault(href)

    private companion object {
        const val TIMEOUT_MS = 8_000
        const val MAX_BYTES = 512 * 1024
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
    }
}
