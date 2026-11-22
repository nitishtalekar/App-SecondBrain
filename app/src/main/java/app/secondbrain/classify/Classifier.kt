package app.secondbrain.classify

import kotlin.math.min

/** Everything we know about an item, split by where the evidence came from. */
data class ClassifyInput(
    val text: String = "",
    val url: String? = null,
    val pageTitle: String? = null,
    val pageDescription: String? = null,
    val pageKeywords: String? = null,
    val ocrText: String? = null,
    val imageLabels: List<String> = emptyList(),
)

data class CategoryScore(
    val categoryId: String,
    val score: Double,
    /** Human-readable evidence, strongest first. */
    val reasons: List<String>,
)

data class Classification(
    /** Chosen category, or null when nothing is confident enough (Inbox). */
    val categoryId: String?,
    val confidence: Double,
    /** Every category that matched at all, best first. */
    val ranked: List<CategoryScore>,
) {
    val reasons: List<String> get() = ranked.firstOrNull { it.categoryId == categoryId }?.reasons.orEmpty()
}

/**
 * Deterministic, explainable, weighted rule matcher. No models, no network.
 *
 * score(category) = sum over rules of weight x fieldMultiplier x repeatBoost(matchCount)
 */
class Classifier(private val config: CategoryConfig) {

    private val compiledRegex = HashMap<String, Regex?>()

    fun classify(input: ClassifyInput): Classification {
        val evidence = buildEvidence(input)
        val scores = config.categories.mapNotNull { category -> score(category, evidence) }
            .sortedByDescending { it.score }

        val best = scores.firstOrNull() ?: return Classification(null, 0.0, emptyList())
        val second = scores.getOrNull(1)?.score ?: 0.0
        val bestDef = config.categories.first { it.id == best.categoryId }
        val minScore = bestDef.minScore ?: config.minScore

        val margin = (best.score - second) / best.score
        val confidence = best.score / (best.score + 3.0) * (0.5 + 0.5 * margin)
        val accepted = best.score >= minScore && confidence >= config.confidenceThreshold
        return Classification(if (accepted) best.categoryId else null, confidence, scores)
    }

    private class Evidence(
        val normalized: Map<Field, String>,
        val raw: Map<Field, String>,
        val host: String?,
        val labels: List<String>,
    )

    private fun buildEvidence(input: ClassifyInput): Evidence {
        val url = input.url?.trim().orEmpty()
        val raw = mapOf(
            Field.TEXT to input.text,
            Field.URL to url,
            Field.META to listOfNotNull(input.pageTitle, input.pageDescription, input.pageKeywords).joinToString("\n"),
            Field.OCR to input.ocrText.orEmpty(),
            Field.LABELS to input.imageLabels.joinToString("\n"),
        )
        return Evidence(
            normalized = raw.mapValues { normalize(it.value) },
            raw = raw,
            host = hostOf(url),
            labels = input.imageLabels.map { it.trim().lowercase() },
        )
    }

    private fun score(category: CategoryDef, evidence: Evidence): CategoryScore? {
        val contributions = ArrayList<Pair<String, Double>>()
        for (rule in category.rules) {
            val value = rule.value.trim()
            if (value.isEmpty()) continue
            val points = when (rule.type) {
                RuleType.KEYWORD -> keywordPoints(rule, value, evidence)
                RuleType.REGEX -> regexPoints(rule, value, evidence)
                RuleType.DOMAIN -> domainPoints(rule, value, evidence)
                RuleType.LABEL -> labelPoints(rule, value, evidence)
            }
            if (points > 0.0) contributions += describe(rule) to points
        }
        if (contributions.isEmpty()) return null
        val merged = contributions.groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }
        return CategoryScore(
            categoryId = category.id,
            score = contributions.sumOf { it.second },
            reasons = merged.entries.sortedByDescending { it.value }.map { it.key }.take(4),
        )
    }

    private fun keywordPoints(rule: Rule, value: String, evidence: Evidence): Double {
        val needle = normalize(value).trim()
        if (needle.isEmpty()) return 0.0
        val fields = rule.field?.let { listOf(it) } ?: KEYWORD_FIELDS
        var total = 0.0
        for (field in fields) {
            // Labels and URL are matched as bags of words just like free text.
            val count = countWord(evidence.normalized[field].orEmpty(), needle)
            if (count > 0) total += rule.weight * FIELD_WEIGHT.getValue(field) * repeatBoost(count)
        }
        return total
    }

    private fun regexPoints(rule: Rule, value: String, evidence: Evidence): Double {
        val regex = compiledRegex.getOrPut(value) {
            runCatching { Regex(value, setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)) }.getOrNull()
        } ?: return 0.0
        val fields = rule.field?.let { listOf(it) } ?: listOf(Field.TEXT, Field.URL, Field.OCR)
        var total = 0.0
        for (field in fields) {
            val text = evidence.raw[field].orEmpty()
            if (text.isEmpty()) continue
            val count = regex.findAll(text).take(5).count()
            if (count > 0) total += rule.weight * FIELD_WEIGHT.getValue(field) * repeatBoost(count)
        }
        return total
    }

    private fun domainPoints(rule: Rule, value: String, evidence: Evidence): Double {
        val host = evidence.host ?: return 0.0
        val domain = value.lowercase().removePrefix("www.")
        return if (host == domain || host.endsWith(".$domain")) rule.weight else 0.0
    }

    private fun labelPoints(rule: Rule, value: String, evidence: Evidence): Double {
        val target = value.trim().lowercase()
        return if (evidence.labels.any { it == target }) rule.weight else 0.0
    }

    private fun describe(rule: Rule): String = when (rule.type) {
        RuleType.DOMAIN -> rule.value
        RuleType.LABEL -> "image: ${rule.value}"
        RuleType.REGEX -> "pattern"
        RuleType.KEYWORD -> rule.value
    }

    private fun repeatBoost(count: Int): Double = 1.0 + 0.25 * (min(count, 5) - 1)

    companion object {
        private val KEYWORD_FIELDS = listOf(Field.TEXT, Field.URL, Field.META, Field.OCR, Field.LABELS)

        private val FIELD_WEIGHT = mapOf(
            Field.TEXT to 1.0,
            Field.URL to 0.8,
            Field.META to 0.9,
            Field.OCR to 0.8,
            Field.LABELS to 1.0,
        )

        /** Lowercase, strip everything but letters and digits, and pad so word boundaries are spaces. */
        fun normalize(text: String): String {
            val sb = StringBuilder(text.length + 2).append(' ')
            var lastSpace = true
            for (ch in text.lowercase()) {
                if (ch.isLetterOrDigit()) {
                    sb.append(ch); lastSpace = false
                } else if (ch != '\'' && ch != '’' && !lastSpace) {
                    sb.append(' '); lastSpace = true
                }
            }
            if (!lastSpace) sb.append(' ')
            return sb.toString()
        }

        /**
         * Counts whole-word occurrences. Needles of 5+ chars also match as a word prefix
         * ("recipe" -> "recipes"); shorter ones match exactly or with a plural "s".
         */
        internal fun countWord(haystack: String, needle: String): Int {
            if (haystack.isEmpty()) return 0
            val prefixOk = needle.length >= 5
            var count = 0
            var from = 0
            val start = " $needle"
            while (true) {
                val i = haystack.indexOf(start, from)
                if (i < 0) break
                val end = i + start.length
                val boundary = end >= haystack.length || haystack[end] == ' '
                val plural = end < haystack.length && haystack[end] == 's' &&
                    (end + 1 >= haystack.length || haystack[end + 1] == ' ')
                if (prefixOk || boundary || plural) count++
                from = i + 1
            }
            return count
        }

        fun hostOf(url: String): String? {
            if (url.isBlank()) return null
            val withoutScheme = url.substringAfter("://", url)
            val host = withoutScheme.substringBefore('/').substringBefore('?').substringBefore('#')
                .substringAfterLast('@').substringBefore(':').lowercase().removePrefix("www.")
            return host.takeIf { it.contains('.') }
        }
    }
}
