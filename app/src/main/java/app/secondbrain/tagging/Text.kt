package app.secondbrain.tagging

import java.text.Normalizer

object Text {
    private val latinMarks = Regex("[\\u0300-\\u036f]+")

    /** "Café" -> "cafe". Only strips Latin diacritics, so other scripts are left intact. */
    fun fold(text: String): String =
        Normalizer.normalize(
            Normalizer.normalize(text, Normalizer.Form.NFD).replace(latinMarks, ""),
            Normalizer.Form.NFC,
        ).lowercase()

    /** Folded, every run of non-alphanumerics collapsed to one space, padded so word boundaries are spaces. */
    fun normalize(text: String): String {
        val folded = fold(text)
        val sb = StringBuilder(folded.length + 2).append(' ')
        var lastSpace = true
        for (ch in folded) {
            val type = Character.getType(ch)
            val partOfWord = ch.isLetterOrDigit() ||
                type == Character.NON_SPACING_MARK.toInt() || type == Character.COMBINING_SPACING_MARK.toInt()
            if (partOfWord) {
                sb.append(ch)
                lastSpace = false
            } else if (ch != '\'' && ch != '’' && !lastSpace) {
                sb.append(' ')
                lastSpace = true
            }
        }
        if (!lastSpace) sb.append(' ')
        return sb.toString()
    }

    /** Crude plural fold so "recipes" and "recipe" count as the same word. */
    fun singular(word: String): String = when {
        word.length > 4 && word.endsWith("ies") -> word.dropLast(3) + "y"
        word.length > 4 && listOf("ches", "shes", "xes", "sses").any { word.endsWith(it) } -> word.dropLast(2)
        word.length > 3 && word.endsWith("s") && listOf("ss", "us", "is").none { word.endsWith(it) } -> word.dropLast(1)
        else -> word
    }

    fun hostOf(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val withoutScheme = url.substringAfter("://", url)
        val host = withoutScheme.substringBefore('/').substringBefore('?').substringBefore('#')
            .substringAfterLast('@').substringBefore(':').lowercase().removePrefix("www.")
        return host.takeIf { it.contains('.') }
    }

    fun hostMatches(host: String?, domain: String): Boolean {
        if (host == null) return false
        val d = domain.lowercase().removePrefix("www.")
        return host == d || host.endsWith(".$d")
    }
}
