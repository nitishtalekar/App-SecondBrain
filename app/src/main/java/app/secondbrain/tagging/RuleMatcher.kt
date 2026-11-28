package app.secondbrain.tagging

import kotlin.math.min

/** Everything known about an item that tags can be derived from. */
data class TagInput(
    val type: ItemType = ItemType.TEXT,
    /** The user's own words (title and note), or the recognised text of a scanned post. */
    val text: String = "",
    val url: String? = null,
    val pageTitle: String? = null,
    val pageDescription: String? = null,
    val pageKeywords: String? = null,
    val siteName: String? = null,
    val ocrText: String? = null,
    val imageLabels: List<String> = emptyList(),
    /** True when [text] was read out of a photo rather than written. */
    val scanned: Boolean = false,
    val createdAt: Long = 0,
)

data class RuleMatch(
    val tag: String,
    val score: Double,
    /** Human-readable evidence, strongest first. */
    val reasons: List<String>,
)

/**
 * Deterministic, explainable, weighted rule matcher. No models, no network. Every tag that clears its
 * threshold is returned, so one item can carry many.
 *
 * score(tag) = sum over rules of weight x fieldMultiplier x repeatBoost(matchCount)
 */
class RuleMatcher(private val rules: TagRules) {

    private val compiledRegex = HashMap<String, Regex?>()
    private val definitions = rules.tags.filter { !it.off }.map { it to it.allRules() }

    fun match(input: TagInput): List<RuleMatch> {
        val evidence = buildEvidence(input)
        val direct = definitions.mapNotNull { (def, defRules) ->
            score(def, defRules, evidence)?.takeIf { it.score >= (def.minScore ?: rules.minScore) }
        }.sortedByDescending { it.score }

        val out = LinkedHashMap<String, RuleMatch>()
        direct.forEach { out[it.tag] = it }
        val byName = rules.tags.associateBy { normalizeTag(it.tag) }
        fun imply(from: String, seen: MutableSet<String>) {
            byName[from]?.implies?.forEach { raw ->
                val parent = normalizeTag(raw)
                if (parent.isEmpty() || !seen.add(parent) || byName[parent]?.off == true) return@forEach
                if (parent !in out) out[parent] = RuleMatch(parent, rules.minScore, listOf("part of ${tagLabel(from)}"))
                imply(parent, seen)
            }
        }
        direct.forEach { imply(it.tag, mutableSetOf(it.tag)) }
        return out.values.toList()
    }

    private class Evidence(
        val normalized: Map<Field, String>,
        val raw: Map<Field, String>,
        val host: String?,
        val labels: List<String>,
    )

    private fun buildEvidence(input: TagInput): Evidence {
        val url = input.url?.trim().orEmpty()
        val raw = mapOf(
            Field.TEXT to if (input.scanned) "" else input.text,
            Field.URL to url,
            Field.META to listOfNotNull(input.pageTitle, input.pageDescription, input.pageKeywords, input.siteName).joinToString("\n"),
            Field.OCR to listOfNotNull(input.ocrText, input.text.takeIf { input.scanned }).joinToString("\n"),
            Field.LABELS to input.imageLabels.joinToString("\n"),
        )
        return Evidence(
            normalized = raw.mapValues { Text.normalize(it.value) },
            raw = raw,
            host = Text.hostOf(url),
            labels = input.imageLabels.map { it.trim().lowercase() },
        )
    }

    private fun score(def: TagDef, defRules: List<Rule>, evidence: Evidence): RuleMatch? {
        val contributions = ArrayList<Pair<String, Double>>()
        for (rule in defRules) {
            val value = rule.value.trim()
            if (value.isEmpty()) continue
            val points = when (rule.type) {
                RuleType.KEYWORD -> keywordPoints(rule, value, evidence)
                RuleType.REGEX -> regexPoints(rule, value, evidence)
                RuleType.DOMAIN -> if (Text.hostMatches(evidence.host, value)) rule.weight else 0.0
                RuleType.LABEL -> if (value.lowercase() in evidence.labels) rule.weight else 0.0
            }
            if (points > 0.0) contributions += describe(rule) to points
        }
        val name = normalizeTag(def.tag)
        if (contributions.isEmpty() || name.isEmpty()) return null
        val merged = contributions.groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }
        return RuleMatch(
            tag = name,
            score = contributions.sumOf { it.second },
            reasons = merged.entries.sortedByDescending { it.value }.map { it.key }.take(3),
        )
    }

    private fun keywordPoints(rule: Rule, value: String, evidence: Evidence): Double {
        val needle = Text.normalize(value).trim()
        if (needle.isEmpty()) return 0.0
        val fields = rule.field?.let { listOf(it) } ?: KEYWORD_FIELDS
        var total = 0.0
        for (field in fields) {
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

    private fun describe(rule: Rule): String = when (rule.type) {
        RuleType.DOMAIN -> rule.value
        RuleType.LABEL -> "photo shows ${rule.value.lowercase()}"
        RuleType.REGEX -> "how it is written"
        RuleType.KEYWORD -> "“${rule.value}”"
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

        /**
         * Counts whole-word occurrences in normalised text. Needles of 5+ chars also match as a word
         * prefix ("recipe" -> "recipes"); shorter ones match exactly or with a plural "s".
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
    }
}
