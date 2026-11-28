package app.secondbrain

import app.secondbrain.tagging.RuleList
import app.secondbrain.tagging.TagDef
import app.secondbrain.tagging.TagRules
import app.secondbrain.tagging.canMatch
import app.secondbrain.tagging.cleanEntries
import app.secondbrain.tagging.find
import app.secondbrain.tagging.tryRule
import app.secondbrain.tagging.withSaved
import app.secondbrain.tagging.without
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RuleEditingTest {

    private val books = TagDef("books", words = listOf("novel"), hints = listOf("author", "chapter"))
    private val poetry = TagDef("poetry", implies = listOf("books"), words = listOf("haiku"))
    private val builtIn = TagRules(tags = listOf(books, poetry))

    @Test fun `typed entries are tidied`() {
        assertEquals(listOf("night train", "rail"), cleanEntries(RuleList.WORDS, " Night Train, rail,, RAIL "))
        assertEquals(listOf("seat61.com"), cleanEntries(RuleList.DOMAINS, "https://www.Seat61.com/italy?x=1, not a site"))
        assertEquals(listOf("Train station"), cleanEntries(RuleList.LABELS, "train station"))
        assertEquals(listOf("loose-ends"), cleanEntries(RuleList.IMPLIES, "#Loose  Ends"))
    }

    @Test fun `only changes to built-in tags are kept`() {
        val changed = books.copy(words = listOf("novel", "saga"))
        val own = TagRules().withSaved(changed, builtIn)
        assertEquals(changed, own.find("books"))
        assertNull(own.withSaved(books, builtIn).find("books"))
        assertNull(own.without("books").find("books"))
    }

    @Test fun `trying a rule shows whether text earns the tag`() {
        assertNotNull(builtIn.tryRule(books, "a lovely novel"))
        assertNull(builtIn.tryRule(books, "one author"))
        assertNotNull(builtIn.tryRule(books, "the author of this chapter"))
        // The edited definition is what gets tried, links included.
        val sites = books.copy(domains = listOf("example.org"))
        assertNotNull(builtIn.tryRule(sites, "https://example.org/a"))
    }

    @Test fun `a tag switched off is never added, even when implied`() {
        assertNull(builtIn.tryRule(books.copy(off = true), "a lovely novel"))
        val rules = TagRules(tags = listOf(books.copy(off = true), poetry))
        assertNull(rules.tryRule(books.copy(off = true), "a haiku"))
        assertNotNull(rules.tryRule(poetry, "a haiku"))
    }

    @Test fun `off survives export and import`() {
        val exported = TagRules(tags = listOf(books.copy(off = true))).encode()
        assertEquals(true, TagRules.parse(exported).find("books")?.off)
    }

    @Test fun `a tag with nothing to look for cannot match`() {
        assertFalse(TagDef("empty").canMatch)
    }
}
