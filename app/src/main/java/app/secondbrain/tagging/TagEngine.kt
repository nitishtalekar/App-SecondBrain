package app.secondbrain.tagging

import java.util.Calendar
import java.util.Locale

data class TagResult(val tags: List<Tag>, val action: ItemAction?)

/**
 * Gives everything as many useful tags as can honestly be derived on-device: topics from the rules,
 * names and keywords from the content, the source site, the format, and when it was saved.
 */
class TagEngine(rules: TagRules) {

    private val matcher = RuleMatcher(rules)

    fun tag(input: TagInput): TagResult {
        val tags = LinkedHashMap<String, Tag>()
        val stems = HashSet<String>()

        fun add(raw: String, kind: TagKind, why: String?) {
            val name = normalizeTag(raw)
            if (name.length < 2 || name in tags) return
            // "book" adds nothing next to "books".
            if (!stems.add(Text.singular(name)) && kind != TagKind.MINE) return
            tags[name] = Tag(name, kind, why)
        }

        val written = listOfNotNull(input.text, input.ocrText).joinToString("\n")
        Keywords.hashtags(written).forEach { add(it, TagKind.MINE, "Marked in your text") }

        matcher.match(input).forEach { add(it.tag, TagKind.TOPIC, "Matched ${it.reasons.joinToString(", ")}") }

        input.imageLabels.take(MAX_LABELS).forEach { add(it, TagKind.KEYWORD, "Recognised in the photo") }

        val names = Keywords.names(if (input.scanned) "" else input.text, MAX_NAMES) +
            Keywords.names(listOfNotNull(input.pageTitle, input.pageDescription).joinToString(". "), MAX_PAGE_NAMES)
        names.forEach { add(it, TagKind.KEYWORD, "Name in the text") }

        input.pageKeywords?.split(',')
            ?.map { it.trim() }
            ?.filter { it.length in 3..24 && it.count { c -> c == ' ' } <= 1 && it.lowercase() !in GENERIC_META }
            ?.take(MAX_PAGE_KEYWORDS)
            ?.forEach { add(it, TagKind.KEYWORD, "Listed by the web page") }

        // Words already covered by a name ("robert", "frost") should not appear again on their own.
        val covered = names.flatMap { it.lowercase().split(' ') }.map(Text::singular).toSet()
        Keywords.words(
            listOf(
                Keywords.Source(if (input.scanned) "" else input.text, 3.0),
                Keywords.Source(input.pageTitle.orEmpty(), 2.5),
                Keywords.Source(input.pageDescription.orEmpty(), 1.0),
                Keywords.Source(listOfNotNull(input.ocrText, input.text.takeIf { input.scanned }).joinToString("\n"), 1.0, noisy = true),
            ),
            MAX_WORDS,
        ).filter { Text.singular(it) !in covered }
            .forEach { add(it, TagKind.KEYWORD, "Word in the text") }

        // A page that couldn't be read still names itself in its address.
        if (input.pageTitle == null) {
            addressWords(input.url).forEach { add(it, TagKind.KEYWORD, "Word in the address") }
        }

        Text.hostOf(input.url)?.let { host -> add(sourceName(host), TagKind.SOURCE, host) }

        formats(input).forEach { add(it, TagKind.FORMAT, null) }

        if (input.createdAt > 0) {
            val calendar = Calendar.getInstance().apply { timeInMillis = input.createdAt }
            add(calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH).orEmpty(), TagKind.TIME, null)
            add(calendar.get(Calendar.YEAR).toString(), TagKind.TIME, null)
        }

        return TagResult(tags.values.sortedBy { it.kind.ordinal }, ActionDetector.detect(input))
    }

    /** Readable words from the last part of the path; ids and hashes are skipped. */
    private fun addressWords(url: String?): List<String> {
        val path = url?.substringAfter("://")?.substringBefore('?')?.substringBefore('#')?.substringAfter('/', "").orEmpty()
        val slug = path.split('/').lastOrNull { it.length > 3 }?.substringBeforeLast('.') ?: return emptyList()
        val words = slug.split('-', '_', '+').filter { word -> word.length in 3..24 && word.all { it.isLetter() } }
        // A single mixed-case token is far more likely an id than a word.
        if (words.size == 1 && words[0].drop(1).any { it.isUpperCase() }) return emptyList()
        return Keywords.words(listOf(Keywords.Source(words.joinToString(" "), 1.0, noisy = true)), MAX_ADDRESS_WORDS)
    }

    private fun formats(input: TagInput): List<String> = buildList {
        add(
            when (input.type) {
                ItemType.IMAGE -> "photo"
                ItemType.LINK -> "link"
                ItemType.TEXT -> "note"
            },
        )
        if (input.scanned) add("from-photo")
        val all = listOfNotNull(input.text, input.ocrText).joinToString("\n")
        if (listLine.findAll(all).take(2).count() >= 2) add("list")
        if (money.containsMatchIn(all)) add("price")
        if (email.containsMatchIn(all) || phone.containsMatchIn(all)) add("contact")
    }

    companion object {
        /** Bumped when tagging changes enough that saved items should be tagged again. */
        const val VERSION = 1

        private const val MAX_LABELS = 5
        private const val MAX_NAMES = 4
        private const val MAX_PAGE_NAMES = 2
        private const val MAX_PAGE_KEYWORDS = 5
        private const val MAX_WORDS = 7
        private const val MAX_ADDRESS_WORDS = 4

        private val listLine = Regex("(?m)^\\s*([-*•]|\\d{1,2}[.)]|\\[[ x]?])\\s+\\S")
        private val money = Regex("([$€£₹¥]\\s?\\d)|(\\b(rs\\.?|inr|usd|eur)\\s?\\d)|(\\d\\s?(usd|eur|inr|dollars|rupees)\\b)", RegexOption.IGNORE_CASE)
        private val email = Regex("[\\w.+-]+@[\\w-]+\\.[\\w.-]+")
        private val phone = Regex("(?<![\\w/])(\\+\\d{1,3}[\\s-]?)?(\\(\\d{3}\\)|\\d{3})[\\s-]?\\d{3}[\\s-]?\\d{4}(?![\\w/])")

        private val GENERIC_META = setOf("article", "website", "video", "video.other", "product", "profile", "object", "music.song", "home", "news", "blog")
        private val SECOND_LEVEL = setOf("co", "com", "org", "net", "gov", "ac", "edu")
        private val ALIASES = mapOf("youtu.be" to "youtube", "goo.gl" to "google", "t.co" to "twitter", "x.com" to "twitter")

        /** "cooking.nytimes.com" -> "nytimes", "bbc.co.uk" -> "bbc". */
        fun sourceName(host: String): String {
            ALIASES[host]?.let { return it }
            val parts = host.split('.')
            if (parts.size < 2) return host
            var index = parts.size - 2
            if (parts[index] in SECOND_LEVEL && index > 0) index--
            return parts[index]
        }
    }
}
