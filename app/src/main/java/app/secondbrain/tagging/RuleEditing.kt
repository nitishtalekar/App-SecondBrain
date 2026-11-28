package app.secondbrain.tagging

import app.secondbrain.ingest.LinkParser

/* Pure logic behind the in-app tag rule editor. */

/** The lists of a [TagDef] that can be edited in the app. */
enum class RuleList { WORDS, HINTS, DOMAINS, LABELS, IMPLIES }

fun TagDef.entries(list: RuleList): List<String> = when (list) {
    RuleList.WORDS -> words
    RuleList.HINTS -> hints
    RuleList.DOMAINS -> domains
    RuleList.LABELS -> labels
    RuleList.IMPLIES -> implies
}

fun TagDef.withEntries(list: RuleList, values: List<String>): TagDef = when (list) {
    RuleList.WORDS -> copy(words = values)
    RuleList.HINTS -> copy(hints = values)
    RuleList.DOMAINS -> copy(domains = values)
    RuleList.LABELS -> copy(labels = values)
    RuleList.IMPLIES -> copy(implies = values)
}

/** Turns what was typed (one entry, or several separated by commas) into the stored form. */
fun cleanEntries(list: RuleList, raw: String): List<String> =
    raw.split(',', '\n').map { entry ->
        val text = entry.trim()
        when (list) {
            RuleList.WORDS, RuleList.HINTS -> text.lowercase()
            // A pasted address is cut down to its host.
            RuleList.DOMAINS -> text.lowercase().substringAfter("://").substringBefore('/').substringBefore('?')
                .removePrefix("www.").takeIf { '.' in it && ' ' !in it }.orEmpty()
            // Matched without regard to case, and reads best as the photo reader writes it.
            RuleList.LABELS -> text.replaceFirstChar { it.titlecase() }
            RuleList.IMPLIES -> normalizeTag(text)
        }
    }.filter { it.isNotEmpty() }.distinct()

/** A tag needs at least one thing to look for. */
val TagDef.canMatch: Boolean get() = allRules().isNotEmpty()

fun TagRules.find(name: String): TagDef? = tags.firstOrNull { normalizeTag(it.tag) == name }

/** The user's own file after saving [def]: a definition identical to the built-in one needn't be kept. */
fun TagRules.withSaved(def: TagDef, builtIn: TagRules): TagRules =
    if (builtIn.find(normalizeTag(def.tag)) == def) without(normalizeTag(def.tag)) else merge(TagRules(version, minScore, listOf(def)))

fun TagRules.without(name: String): TagRules = copy(tags = tags.filter { normalizeTag(it.tag) != name })

/** Whether [text] would earn the tag [def] if [def] were saved, and why. Null means it wouldn't. */
fun TagRules.tryRule(def: TagDef, text: String): RuleMatch? {
    if (text.isBlank()) return null
    val parsed = LinkParser.parse(text)
    val input = TagInput(
        type = if (parsed.url != null) ItemType.LINK else ItemType.TEXT,
        text = parsed.note,
        url = parsed.url,
    )
    val name = normalizeTag(def.tag)
    return RuleMatcher(merge(TagRules(version, minScore, listOf(def)))).match(input).firstOrNull { it.tag == name }
}
