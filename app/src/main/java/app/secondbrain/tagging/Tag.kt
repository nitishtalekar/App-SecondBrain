package app.secondbrain.tagging

import kotlinx.serialization.Serializable

enum class ItemType { TEXT, LINK, IMAGE }

/** Where a tag came from. The declaration order is the display order, most meaningful first. */
@Serializable
enum class TagKind {
    /** Added or written (#hashtag) by the user. */
    MINE,
    /** Matched by the tag rules. */
    TOPIC,
    /** A word or name lifted from the content itself. */
    KEYWORD,
    /** The website a link came from. */
    SOURCE,
    /** What kind of thing this is: note, link, photo, list... */
    FORMAT,
    /** Month and year it was saved. */
    TIME,
}

@Serializable
data class Tag(val name: String, val kind: TagKind, val why: String? = null)

/** Something waiting on the user. Orthogonal to tags so it can be completed. */
enum class ItemAction(
    val pending: String,
    val verb: String,
    val completed: String,
    /** Virtual tag used by search. */
    val searchTag: String,
) {
    READ("To read", "Mark as read", "Read", "to-read"),
    WATCH("To watch", "Mark as watched", "Watched", "to-watch"),
    BUY("To buy", "Mark as bought", "Bought", "to-buy"),
    DO("To do", "Mark as done", "Done", "to-do"),
}

private const val MAX_TAG_LENGTH = 32

/** Lowercase, hyphenated, no leading '#'. Letters and digits of any script survive. */
fun normalizeTag(raw: String): String =
    raw.lowercase()
        .replace(Regex("['’]"), "")
        .replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), "-")
        .trim('-')
        .take(MAX_TAG_LENGTH)
        .trim('-')

/** Several tags typed at once, separated by commas or spaces. */
private val TAG_SPELLINGS = mapOf(
    "ai" to "AI", "diy" to "DIY", "tv" to "TV", "youtube" to "YouTube", "github" to "GitHub", "linkedin" to "LinkedIn",
)

/** How a tag reads on screen: "loose ends" is "Loose Ends". Stored names stay lowercase. */
fun tagLabel(name: String): String =
    name.split(' ', '-', '_').filter { it.isNotEmpty() }.joinToString(" ") { word ->
        TAG_SPELLINGS[word] ?: word.replaceFirstChar { it.titlecase() }
    }

fun parseTags(raw: String): List<String> =
    raw.split(',', ' ', '\n').map(::normalizeTag).filter { it.isNotEmpty() }.distinct()
