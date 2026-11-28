package app.secondbrain.search

import app.secondbrain.data.Entry
import app.secondbrain.tagging.ItemAction
import app.secondbrain.tagging.ItemType
import app.secondbrain.tagging.TagKind
import app.secondbrain.tagging.Text
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** One-tap filters on the state of an item. */
enum class StatusFilter(val label: String) {
    DO("To do"), READ("To read"), BUY("To buy"), WATCH("To watch"), DONE("Done");

    fun matches(entry: Entry): Boolean = when (this) {
        DONE -> entry.done
        DO -> entry.openAction == ItemAction.DO
        READ -> entry.openAction == ItemAction.READ
        BUY -> entry.openAction == ItemAction.BUY
        WATCH -> entry.openAction == ItemAction.WATCH
    }
}

data class SearchParams(
    val query: String = "",
    val status: StatusFilter? = null,
    val type: ItemType? = null,
    /** Every one of these must be on the item. */
    val tags: Set<String> = emptySet(),
) {
    val isEmpty get() = query.isBlank() && status == null && type == null && tags.isEmpty()
}

data class SearchHit(val entry: Entry, val score: Double, val snippet: String? = null)

data class TagCount(val name: String, val count: Int)

private const val FUZZY_MIN = 4

/** Pre-normalised text of one entry, built once so typing stays fast. */
class SearchDoc(val entry: Entry) {
    private val item = entry.item

    val tagNames: Set<String> = entry.tags.map { it.name }.toSet()

    private val virtualTags: List<String> = buildList {
        item.action?.let { add(if (entry.done) "done completed ${it.completed}" else it.searchTag) }
    }

    internal val tags = Text.normalize((tagNames + virtualTags).joinToString(" "))
    internal val title = Text.normalize(listOfNotNull(item.title, entry.title, item.linkTitle).joinToString("\n"))
    internal val body = Text.normalize(item.body)
    internal val other = Text.normalize(
        listOfNotNull(
            item.ocrText, item.url, item.linkDescription, item.linkSiteName, item.linkKeywords, item.imageLabels,
            SimpleDateFormat("EEEE d MMMM yyyy MMM", Locale.ENGLISH).format(Date(item.createdAt)),
        ).joinToString("\n"),
    )
    internal val words: List<String> = (tags + title).split(' ').filter { it.length >= FUZZY_MIN }.distinct()
}

/**
 * Searches words, tags and everything else at once. All terms must match somewhere. Supports
 * `#tag`, `"exact phrase"`, `-exclude`, `today`, `yesterday` and one-letter typos.
 */
object SearchEngine {

    private data class Term(val text: String, val tagOnly: Boolean, val exclude: Boolean, val phrase: Boolean)

    private val tokenRegex = Regex("""(-)?(#)?(?:"([^"]*)"?|(\S+))""")

    fun search(docs: List<SearchDoc>, params: SearchParams, now: Long = System.currentTimeMillis()): List<SearchHit> {
        var query = params.query
        var window: LongRange? = null
        for ((word, daysAgo) in listOf("today" to 0, "yesterday" to 1)) {
            val pattern = Regex("(?i)(?<!\\S)$word(?!\\S)")
            if (pattern.containsMatchIn(query)) {
                window = dayRange(now, daysAgo)
                query = query.replace(pattern, " ")
            }
        }
        val terms = parse(query)

        return docs.mapNotNull { doc ->
            val entry = doc.entry
            if (params.type != null && entry.item.type != params.type) return@mapNotNull null
            if (params.status != null && !params.status.matches(entry)) return@mapNotNull null
            if (!doc.tagNames.containsAll(params.tags)) return@mapNotNull null
            if (window != null && entry.item.createdAt !in window) return@mapNotNull null
            var total = 0.0
            for (term in terms) {
                val score = score(doc, term)
                if (term.exclude) {
                    if (score > 0) return@mapNotNull null
                } else {
                    if (score <= 0) return@mapNotNull null
                    total += score
                }
            }
            SearchHit(entry, total, snippet(entry, terms.firstOrNull { !it.exclude && !it.tagOnly }?.text))
        }.sortedWith(compareByDescending<SearchHit> { it.score }.thenByDescending { it.entry.item.createdAt })
    }

    /** The tags that appear most among [entries], for grouping and narrowing down. */
    fun tagCounts(entries: List<Entry>, exclude: Set<String> = emptySet()): List<TagCount> {
        val counts = HashMap<String, Int>()
        for (entry in entries) {
            for (tag in entry.tags) {
                if (tag.kind == TagKind.TIME || tag.kind == TagKind.FORMAT || tag.name in exclude) continue
                counts[tag.name] = (counts[tag.name] ?: 0) + 1
            }
        }
        return counts.map { TagCount(it.key, it.value) }
            .sortedWith(compareByDescending<TagCount> { it.count }.thenBy { it.name })
    }

    /** The word still being typed at the end of [query]; empty after a space. */
    fun typedWord(query: String): String = query.takeLastWhile { !it.isWhitespace() }

    /**
     * Tags that complete the word being typed, taken from what the rest of the search still matches,
     * so choosing one never leads to an empty list. Tags starting with the word come first.
     */
    fun suggestTags(docs: List<SearchDoc>, params: SearchParams, limit: Int = 8): List<TagCount> {
        val typed = typedWord(params.query)
        // Exclusions and quoted phrases are deliberate; leave them alone.
        if (typed.startsWith("-") || params.query.count { it == '"' } % 2 == 1) return emptyList()
        val needle = typed.removePrefix("#").lowercase()
        if (needle.isEmpty()) return emptyList()
        val rest = params.copy(query = params.query.dropLast(typed.length))
        val counts = tagCounts(search(docs, rest).map { it.entry }, exclude = params.tags)
        val (starting, others) = counts.partition { tag -> tag.name.split(' ', '-').any { it.startsWith(needle) } }
        val containing = if (needle.length >= 3) others.filter { needle in it.name } else emptyList()
        return (starting + containing).take(limit)
    }

    private fun parse(query: String): List<Term> = tokenRegex.findAll(query).mapNotNull { match ->
        val phrase = match.groups[3]?.value
        val text = Text.normalize(phrase ?: match.groups[4]?.value.orEmpty()).trim()
        if (text.isEmpty()) null
        else Term(text, tagOnly = match.groups[2] != null, exclude = match.groups[1] != null, phrase = phrase != null)
    }.toList()

    private fun score(doc: SearchDoc, term: Term): Double {
        if (term.tagOnly) return if (doc.tags.contains(" ${term.text}")) 10.0 else 0.0
        val needles = if (term.phrase) listOf(term.text) else listOf(term.text, Text.singular(term.text)).distinct()
        var best = 0.0
        for (needle in needles) {
            // Excluded words must start a word; "-art" should not hide "party".
            fun fieldScore(field: String, weight: Double): Double = when {
                field.contains(" $needle ") -> weight * 1.5
                field.contains(" $needle") -> weight
                !term.exclude && needle.length >= 3 && field.contains(needle) -> weight * 0.4
                else -> 0.0
            }
            best = maxOf(
                best,
                maxOf(fieldScore(doc.tags, 6.0), fieldScore(doc.title, 5.0)),
                maxOf(fieldScore(doc.body, 3.0), fieldScore(doc.other, 2.0)),
            )
        }
        if (best == 0.0 && !term.exclude && !term.phrase && term.text.length >= FUZZY_MIN &&
            doc.words.any { withinOneEdit(it, term.text) }
        ) best = 0.5
        return best
    }

    /** One substitution, insertion, deletion or swap of neighbours. */
    internal fun withinOneEdit(a: String, b: String): Boolean {
        if (abs(a.length - b.length) > 1) return false
        var i = 0
        while (i < a.length && i < b.length && a[i] == b[i]) i++
        if (i == a.length || i == b.length) return true
        return when {
            a.length == b.length -> a.substring(i + 1) == b.substring(i + 1) ||
                (i + 1 < a.length && a[i] == b[i + 1] && a[i + 1] == b[i] && a.substring(i + 2) == b.substring(i + 2))
            a.length > b.length -> a.substring(i + 1) == b.substring(i)
            else -> a.substring(i) == b.substring(i + 1)
        }
    }

    /** Shows where the match was found when it isn't already visible in the title. */
    private fun snippet(entry: Entry, term: String?): String? {
        if (term == null || Text.fold(entry.title).contains(term)) return null
        val item = entry.item
        for (text in listOfNotNull(item.body, item.ocrText, item.linkDescription, item.url)) {
            val index = text.lowercase().indexOf(term)
            if (index < 0) continue
            val start = (index - 28).coerceAtLeast(0)
            val end = (index + term.length + 56).coerceAtMost(text.length)
            val prefix = if (start > 0) "…" else ""
            val suffix = if (end < text.length) "…" else ""
            return prefix + text.substring(start, end).replace('\n', ' ').trim() + suffix
        }
        return null
    }

    private fun dayRange(now: Long, daysAgo: Int): LongRange {
        val start = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }
        val from = start.timeInMillis
        start.add(Calendar.DAY_OF_YEAR, 1)
        return from until start.timeInMillis
    }
}
