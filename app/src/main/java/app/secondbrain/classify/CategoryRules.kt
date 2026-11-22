package app.secondbrain.classify

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

@Serializable
data class CategoryDef(
    val id: String,
    val name: String,
    val color: String = "#8C8272",
    /** Minimum raw score this category needs to win. Falls back to [CategoryConfig.minScore]. */
    val minScore: Double? = null,
    val rules: List<Rule> = emptyList(),
)

@Serializable
data class CategoryConfig(
    val version: Int = 1,
    /** Raw score a category needs before it can be assigned automatically. */
    val minScore: Double = 2.0,
    /** Confidence (0..1) below which an item goes to the Inbox instead. */
    val confidenceThreshold: Double = 0.3,
    val categories: List<CategoryDef> = emptyList(),
) {
    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            prettyPrint = true
            encodeDefaults = true
        }

        fun parse(text: String): CategoryConfig = json.decodeFromString(serializer(), text)
    }

    fun encode(): String = json.encodeToString(serializer(), this)
}
