package app.secondbrain.tagging

/**
 * Pulls the words and names worth searching by out of free text, using frequency, position and
 * capitalisation. Purely lexical, so every keyword is something that literally appears in the item.
 */
object Keywords {

    data class Source(val text: String, val weight: Double, val noisy: Boolean = false)

    fun words(sources: List<Source>, limit: Int): List<String> {
        val score = LinkedHashMap<String, Double>()
        val display = HashMap<String, String>()
        for (source in sources) {
            for (match in wordRegex.findAll(stripUrls(source.text))) {
                val word = match.value.lowercase().replace(possessive, "")
                if (!isMeaningful(word, source.noisy)) continue
                val key = Text.singular(word)
                if (key in STOPWORDS) continue
                display.putIfAbsent(key, word)
                // Longer words tend to be the more specific ones.
                score[key] = (score[key] ?: 0.0) + source.weight * (1.0 + minOf(word.length, 10) / 40.0)
            }
        }
        return score.entries.sortedByDescending { it.value }.take(limit).map { display.getValue(it.key) }
    }

    /** Runs of capitalised words that are not just sentence openers: "Robert Frost", "Lisbon". */
    fun names(text: String, limit: Int): List<String> {
        val found = LinkedHashMap<String, Int>()
        for (sentence in stripUrls(text).split(sentenceBreak)) {
            val tokens = wordRegex.findAll(sentence).map { it.value }.toList()
            if (tokens.isEmpty()) continue
            val capitalised = tokens.count { it.first().isUpperCase() }
            // A Title Case Headline says nothing about which words are names.
            if (tokens.size >= 4 && capitalised > tokens.size * 0.6) continue
            var run = mutableListOf<String>()
            var runStart = 0
            fun flush() {
                // A lone capital at the start of a sentence is ordinary punctuation, not a name.
                if (run.isNotEmpty() && !(runStart == 0 && run.size == 1) && run.size <= 3) {
                    val name = run.joinToString(" ")
                    found[name] = (found[name] ?: 0) + 1
                }
                run = mutableListOf()
            }
            tokens.forEachIndexed { index, token ->
                val clean = token.replace(possessive, "")
                val isName = clean.length >= 3 && clean.first().isUpperCase() && clean.drop(1).any { it.isLowerCase() } &&
                    clean.lowercase() !in STOPWORDS
                if (isName) {
                    if (run.isEmpty()) runStart = index
                    run += clean
                } else flush()
            }
            flush()
        }
        return found.entries.sortedByDescending { it.value }.take(limit).map { it.key }
    }

    fun hashtags(text: String): List<String> =
        hashtagRegex.findAll(stripUrls(text)).map { normalizeTag(it.groupValues[1]) }.filter { it.isNotEmpty() }.distinct().toList()

    private fun stripUrls(text: String) = urlRegex.replace(text, " ")

    private fun isMeaningful(word: String, noisy: Boolean): Boolean {
        if (word.length < (if (noisy) 4 else 3) || word.length > 24) return false
        if (word in STOPWORDS) return false
        if (word.none { it.isLetter() }) return false
        // Text recognition produces consonant soup; real Latin-script words have a vowel.
        if (noisy && word.all { it in 'a'..'z' } && word.none { it in "aeiouy" }) return false
        return true
    }

    private val possessive = Regex("['’].*$")
    private val wordRegex = Regex("\\p{L}[\\p{L}\\p{M}'’]*")
    private val sentenceBreak = Regex("[.!?\\n:|•,;()\"“”]+")
    private val hashtagRegex = Regex("(?<![\\p{L}\\p{N}&/])#(\\p{L}[\\p{L}\\p{M}\\p{N}_-]+)")
    private val urlRegex = Regex("""(?i)\b(?:https?://|www\.)[^\s<>"]+""")

    private val STOPWORDS: Set<String> = """
        a able about above across actually add added after again against ago all almost along already also although
        always am among an and another any anyone anything app are around as ask at away back bad be because become been
        before behind being below best better between big bit both bring but buy bought by call called came can cannot
        cant come comes could day days did didnt different do does doesnt doing done dont down during each early easy
        either else end enough etc even evening ever every everyone everything far fast few find first five following for
        forget forgot found four free friday from full further gave get gets getting give given go goes going gone good
        got great had half hand has have havent having he hello help her here hers herself hey high him himself his hour
        hours how however http https if important in inside instead into is isnt it its itself just keep kept kind know
        known last late later least left less let lets like liked likely little long look looked looking lot love made
        make makes making man many may maybe me mean might mine minute minutes miss monday month months more morning most
        mostly move much must my myself near need needed needs neither never new next nice night no nobody none nor not
        note notes nothing now of off often oh ok okay old on once one only onto open or order other others our ours
        ourselves out outside over own page part past pay per perhaps place please post probably put quite rather read
        reading real really remember remind reminder right said same saturday save saved saw say saying says second see
        seem seemed seems seen send set several shall she should show side since single site small so some someone
        something sometime sometimes soon sorry start still stop such sunday sure take taken takes taking tell than thank
        thanks that the their theirs them themselves then there these they thing things think this those though thought
        three through thursday till time times to today together told tomorrow tonight too took top toward try trying
        tuesday turn two under unless until up upon us use used uses using usually very via visit want wanted wants was
        watch way we website wednesday week weekend weeks well went were what whatever when where whether which while who
        whole whom whose why will with within without wont work working works would www year years yes yesterday yet you
        your yours yourself january february march april june july august september october november december com org net
        html amp nbsp untitled copy share shared click here view more read info online official home welcome full latest
        search login sign video image photo picture link article watch subscribe follow todo list open opens closed until daily
        weekly served serves fresh welcome wiki index html pages book booking
    """.split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet()
}
