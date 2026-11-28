package app.secondbrain.tagging

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class RuleType {
    @SerialName("keyword") KEYWORD,
    @SerialName("domain") DOMAIN,
    @SerialName("regex") REGEX,
    @SerialName("label") LABEL,
}

@Serializable
enum class Field {
    @SerialName("text") TEXT,
    @SerialName("url") URL,
    @SerialName("meta") META,
    @SerialName("ocr") OCR,
    @SerialName("labels") LABELS,
}

@Serializable
data class Rule(
    val type: RuleType = RuleType.KEYWORD,
    val value: String,
    val weight: Double = 1.0,
    /** Restrict the rule to one kind of evidence. Null means "anywhere that makes sense for the type". */
    val field: Field? = null,
)

private const val WORD_WEIGHT = 2.5
private const val HINT_WEIGHT = 1.0
private const val DOMAIN_WEIGHT = 4.0
private const val LABEL_WEIGHT = 3.0

/** One automatic tag and the evidence that earns it. The shorthand lists cover the common cases. */
@Serializable
data class TagDef(
    val tag: String,
    /** Broader tags that come along with this one ("recipes" implies "food"). */
    val implies: List<String> = emptyList(),
    /** Raw score needed. Falls back to [TagRules.minScore]. */
    val minScore: Double? = null,
    /** Strong keywords: one is enough. */
    val words: List<String> = emptyList(),
    /** Weak keywords: it takes two. */
    val hints: List<String> = emptyList(),
    /** Link hosts. */
    val domains: List<String> = emptyList(),
    /** Image labels from on-device recognition. */
    val labels: List<String> = emptyList(),
    /** Full control over type, weight and field. */
    val rules: List<Rule> = emptyList(),
    /** Switched off: never added automatically, not even because another tag implies it. */
    val off: Boolean = false,
) {
    fun allRules(): List<Rule> =
        words.map { Rule(RuleType.KEYWORD, it, WORD_WEIGHT) } +
            hints.map { Rule(RuleType.KEYWORD, it, HINT_WEIGHT) } +
            domains.map { Rule(RuleType.DOMAIN, it, DOMAIN_WEIGHT) } +
            labels.map { Rule(RuleType.LABEL, it, LABEL_WEIGHT) } +
            rules
}

@Serializable
data class TagRules(
    val version: Int = 2,
    val minScore: Double = 2.0,
    val tags: List<TagDef> = emptyList(),
) {
    /** Definitions in [other] replace same-named ones here; new ones are appended. */
    fun merge(other: TagRules): TagRules {
        val byName = LinkedHashMap<String, TagDef>()
        tags.forEach { byName[normalizeTag(it.tag)] = it }
        other.tags.forEach { byName[normalizeTag(it.tag)] = it }
        return TagRules(other.version, other.minScore, byName.values.toList())
    }

    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            prettyPrint = true
        }

        fun parse(text: String): TagRules = json.decodeFromString(serializer(), text)
    }
}
