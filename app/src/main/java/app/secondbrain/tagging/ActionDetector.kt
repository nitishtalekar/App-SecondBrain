package app.secondbrain.tagging

/**
 * Decides whether something is waiting on the user: to be read, watched, bought or done. Written
 * intentions win over what a link's website suggests.
 */
object ActionDetector {

    private fun lineRegex(pattern: String) = Regex(pattern, RegexOption.MULTILINE)

    // Lines are normalised first: lowercase, no apostrophes or punctuation, one line per sentence.
    private val explicit = listOf(
        ItemAction.BUY to lineRegex("""\b(to buy|buy list|shopping list|grocery list|groceries list|wish ?list|to order|to purchase)\b"""),
        ItemAction.READ to lineRegex("""\b(to read|reading list|book recommendation|must read|should read|tbr)\b"""),
        ItemAction.WATCH to lineRegex("""\b(to watch|watch ?list|must watch|should watch)\b"""),
    )

    private val leading = listOf(
        ItemAction.BUY to lineRegex("""^(buy|order|purchase|get (some|a|an|more|new)|pick up (some|a|an|more))\b"""),
        ItemAction.READ to lineRegex("""^read(?! (it |this |that )?(yesterday|today|last))\b"""),
        ItemAction.WATCH to lineRegex("""^watch\b"""),
    )

    private val doing = lineRegex(
        """(^(todo|to do|call|email|submit|renew|schedule|cancel|reply|return|send|pay|fix|clean|finish|text|ask|book (a|an|the|my|our|tickets?|flights?|hotels?|table))\b)""" +
            """|\b(todo|remember to|dont forget|do not forget|need to|have to|remind me|follow up)\b""",
    )

    /** Photographed text only counts when it announces itself as a list: a poster saying "Buy now" is not a task. */
    private val scannedHeaders = listOf(
        ItemAction.BUY to lineRegex("""^(to buy|shopping list|grocery list|groceries)\b"""),
        ItemAction.READ to lineRegex("""^(to read|reading list)\b"""),
        ItemAction.WATCH to lineRegex("""^(to watch|watch ?list)\b"""),
        ItemAction.DO to lineRegex("""^(todo|to do( list)?|tasks)$"""),
    )

    private val watchHosts = listOf("youtube.com", "youtu.be", "netflix.com", "vimeo.com", "primevideo.com", "hulu.com", "disneyplus.com", "twitch.tv")
    private val readHosts = listOf("medium.com", "substack.com", "arxiv.org", "dev.to", "goodreads.com", "wikipedia.org", "longreads.com")
    private val shopHosts = listOf("amazon.com", "amazon.in", "amazon.co.uk", "flipkart.com", "ebay.com", "etsy.com", "ikea.com", "aliexpress.com", "myntra.com")
    private val productPath = Regex("""/(dp|gp/product|product|products|p|itm|listing)/""")
    private val articleType = Regex("""\barticle\b""")
    private val checkbox = Regex("""(?m)^\s*(-\s*)?\[ ?]""")
    private val sentenceBreak = Regex("[\\n.!?;]+")

    /** Text wrapped in quote marks, or signed off with a dash and a name: someone else's words. */
    private val quoted = Regex("""\A\s*["“«‘][\s\S]{12,}["”»’](\s*[—–~-]{1,2}\s*\S[^\n]{0,80})?\s*\z""")
    private val signed = Regex("""([.!?…"”»’]|\n)\s*[—–~]\s*\p{Lu}[^\n]{0,60}\s*\z""")

    fun detect(input: TagInput): ItemAction? {
        val someoneElses = quoted.containsMatchIn(input.text) || signed.containsMatchIn(input.text)
        val written = if (someoneElses) "" else lines(input.text)
        if (input.scanned) return scannedHeaders.firstOrNull { it.second.containsMatchIn(written) }?.first

        explicit.firstOrNull { it.second.containsMatchIn(written) }?.let { return it.first }
        // "remember to buy milk" is caught above; here the line itself opens with the verb.
        leading.firstOrNull { it.second.containsMatchIn(written) }?.let { return it.first }
        if (doing.containsMatchIn(written) || checkbox.containsMatchIn(input.text)) return ItemAction.DO

        val host = Text.hostOf(input.url) ?: return null
        fun on(hosts: List<String>) = hosts.any { Text.hostMatches(host, it) }
        val type = input.pageKeywords?.lowercase().orEmpty()
        return when {
            on(shopHosts) && (productPath.containsMatchIn(input.url.orEmpty()) || "product" in type) -> ItemAction.BUY
            on(watchHosts) -> ItemAction.WATCH
            on(readHosts) || articleType.containsMatchIn(type) -> ItemAction.READ
            else -> null
        }
    }

    private fun lines(text: String): String =
        text.split(sentenceBreak).joinToString("\n") { Text.normalize(it).trim() }
}
