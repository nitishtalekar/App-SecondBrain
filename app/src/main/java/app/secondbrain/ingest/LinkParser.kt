package app.secondbrain.ingest

data class ParsedCapture(val url: String?, val note: String)

object LinkParser {
    private val urlRegex = Regex("""(?i)\b((?:https?://|www\.)[^\s<>"]+)""")
    private const val MAX_NOTE_WHEN_LINK = 120

    /**
     * A capture is a link when it contains a URL and the surrounding text is short
     * (a share sheet typically sends "Page title https://…"). Longer prose that merely
     * mentions a URL stays a text note.
     */
    fun parse(input: String): ParsedCapture {
        val text = input.trim()
        val match = urlRegex.find(text) ?: return ParsedCapture(null, text)
        val url = normalize(match.value.trimEnd('.', ',', ')', ';', '!', '?'))
        val rest = text.removeRange(match.range).trim()
        return if (rest.length <= MAX_NOTE_WHEN_LINK) ParsedCapture(url, rest) else ParsedCapture(null, text)
    }

    fun normalize(url: String): String = if (url.startsWith("www.", ignoreCase = true)) "https://$url" else url
}
