package app.secondbrain

import app.secondbrain.tagging.ActionDetector
import app.secondbrain.tagging.ItemAction
import app.secondbrain.tagging.ItemType
import app.secondbrain.tagging.Rule
import app.secondbrain.tagging.RuleType
import app.secondbrain.tagging.TagDef
import app.secondbrain.tagging.TagEngine
import app.secondbrain.tagging.TagInput
import app.secondbrain.tagging.TagKind
import app.secondbrain.tagging.TagRules
import app.secondbrain.tagging.normalizeTag
import app.secondbrain.tagging.parseTags
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagEngineTest {

    private fun names(input: TagInput) = defaultEngine.tag(input).tags.map { it.name }

    private fun text(value: String) = TagInput(text = value)

    private fun action(value: String) = ActionDetector.detect(text(value))

    @Test fun `one thing gets many tags from several sources`() {
        val tags = names(text("Try this pasta carbonara recipe from Rome, ingredients are eggs and pecorino #weekend"))
        assertTrue(tags.toString(), tags.containsAll(listOf("weekend", "recipes", "food", "carbonara", "rome", "pecorino", "note")))
    }

    @Test fun `every default tag name is already normalised and unique`() {
        val all = defaultRules.tags.map { it.tag }
        assertEquals(all, all.map(::normalizeTag))
        assertEquals(all.size, all.distinct().size)
        val known = all.toSet()
        assertTrue(defaultRules.tags.all { def -> def.implies.all { it in known } })
    }

    @Test fun `links carry their source and topic`() {
        val result = defaultEngine.tag(
            TagInput(type = ItemType.LINK, url = "https://www.goodreads.com/book/show/12345", pageTitle = "Dune by Frank Herbert"),
        )
        val tags = result.tags.map { it.name }
        assertTrue(tags.toString(), tags.containsAll(listOf("books", "goodreads", "link", "dune")))
        assertEquals(ItemAction.READ, result.action)
    }

    @Test fun `page keywords become tags`() {
        val tags = names(TagInput(type = ItemType.LINK, url = "https://example.com/x", pageKeywords = "sourdough, fermentation, article"))
        assertTrue(tags.containsAll(listOf("sourdough", "fermentation")))
        assertFalse("article" in tags)
    }

    @Test fun `an unread link is tagged from its address but ids are skipped`() {
        fun on(url: String) = names(TagInput(type = ItemType.LINK, url = url))
        assertTrue(on("https://en.wikipedia.org/wiki/Celestial_globe").containsAll(listOf("celestial", "globe", "wikipedia")))
        assertTrue(on("https://youtu.be/dQwXcQk").none { it == "dqwxcqk" })
    }

    @Test fun `photo labels become tags`() {
        val tags = names(TagInput(type = ItemType.IMAGE, imageLabels = listOf("Food", "Dessert")))
        assertTrue(tags.toString(), tags.containsAll(listOf("food", "dessert", "photo")))
    }

    @Test fun `saved month and year are tags`() {
        val march = Calendar.getInstance().apply { set(2026, Calendar.MARCH, 15, 12, 0) }.timeInMillis
        val tags = names(TagInput(text = "hello there", createdAt = march))
        assertTrue(tags.containsAll(listOf("march", "2026")))
    }

    @Test fun `formats are detected`() {
        val tags = names(text("- milk $4\n- bread\nreach me at a@b.com"))
        assertTrue(tags.toString(), tags.containsAll(listOf("list", "price", "contact", "note")))
    }

    @Test fun `no duplicate or plural-twin tags`() {
        val tags = names(text("book books Book BOOKS isbn"))
        assertEquals(tags, tags.distinct())
        assertFalse(tags.contains("book") && tags.contains("books"))
    }

    @Test fun `tags are ordered by kind`() {
        val kinds = defaultEngine.tag(text("#mine pasta dinner")).tags.map { it.kind.ordinal }
        assertEquals(kinds.sorted(), kinds)
        assertEquals(TagKind.MINE, defaultEngine.tag(text("#mine pasta dinner")).tags.first().kind)
    }

    @Test fun `filler words are not keywords`() {
        val tags = names(text("I really think that we should just do this today"))
        assertTrue(tags.toString(), tags.none { it in listOf("really", "think", "should", "today") })
    }

    @Test fun `things to buy`() {
        assertEquals(ItemAction.BUY, action("Buy new running shoes"))
        assertEquals(ItemAction.BUY, action("remember to buy milk"))
        assertEquals(ItemAction.BUY, action("Shopping list:\neggs\nbread"))
        assertEquals(ItemAction.BUY, action("buy the book Dune"))
    }

    @Test fun `things to read and watch`() {
        assertEquals(ItemAction.READ, action("Read Thinking Fast and Slow"))
        assertEquals(ItemAction.READ, action("Book recommendation: Piranesi"))
        assertEquals(ItemAction.WATCH, action("watch Severance season 2"))
        assertEquals(ItemAction.WATCH, action("That new film is a must watch"))
    }

    @Test fun `things to do`() {
        assertEquals(ItemAction.DO, action("Call mom tomorrow"))
        assertEquals(ItemAction.DO, action("need to renew passport"))
        assertEquals(ItemAction.DO, action("TODO fix the sink"))
        assertEquals(ItemAction.DO, action("Book flights to Goa"))
        assertEquals(ItemAction.DO, action("[ ] water plants"))
    }

    @Test fun `ordinary notes are not actions`() {
        assertNull(action("I read a great quote yesterday"))
        assertNull(action("10 things to do in Lisbon"))
        assertNull(action("The market was busy and the coffee was good"))
        assertNull(action("What if books recommended readers"))
    }

    @Test fun `links suggest actions from their site`() {
        fun on(url: String) = ActionDetector.detect(TagInput(type = ItemType.LINK, url = url))
        assertEquals(ItemAction.WATCH, on("https://youtu.be/abc"))
        assertEquals(ItemAction.READ, on("https://someone.substack.com/p/post"))
        assertEquals(ItemAction.BUY, on("https://www.amazon.in/dp/B0123"))
        assertNull(on("https://www.amazon.in/"))
        assertNull(on("https://example.com"))
    }

    @Test fun `photographed text is only a task when it says it is a list`() {
        fun scanned(value: String) = ActionDetector.detect(TagInput(text = value, scanned = true))
        assertNull(scanned("SALE\nBuy now and call today"))
        assertEquals(ItemAction.BUY, scanned("Shopping list\nrice\ndal"))
    }

    @Test fun `pasted quotes are recognised in their usual shapes`() {
        listOf(
            "\"Simplicity is the ultimate sophistication.\"",
            "“We are what we repeatedly do.\nExcellence, then, is not an act, but a habit.”",
            "‘It is never too late to be what you might have been.’ - George Eliot",
            "The best way to predict the future is to invent it. — Alan Kay",
            "What we know is a drop, what we don't know is an ocean.\n- Isaac Newton",
            "Stay hungry, stay foolish\n~ Stewart Brand",
        ).forEach { assertTrue(it, "quotes" in names(text(it))) }
    }

    @Test fun `ordinary notes are not mistaken for quotes`() {
        listOf(
            "Shopping\n- Eggs\n- Milk",
            "- pick up the parcel from the front desk today\n- Eggs",
            "Dinner on Saturday — Priya",
            "Meet at the station - bring the tickets",
            "Finished \"Dune\" last night",
        ).forEach { assertFalse(it, "quotes" in names(text(it))) }
    }

    @Test fun `a quote is not read as a task`() {
        assertNull(action("\"You do not need to see the whole staircase, just take the first step.\""))
        assertNull(action("Remember to look up at the stars. — Stephen Hawking"))
        assertEquals(ItemAction.DO, action("need to renew the passport"))
    }

    @Test fun `quoted attribution is tagged as a quote`() {
        assertTrue("quotes" in names(text("“The only way out is through.” — Robert Frost")))
    }

    @Test fun `whole word matching avoids substring false positives`() {
        val engine = TagEngine(TagRules(tags = listOf(TagDef("gallery", words = listOf("art")))))
        fun has(value: String) = engine.tag(text(value)).tags.any { it.name == "gallery" }
        assertFalse(has("a party apart"))
        assertTrue(has("modern art museum"))
    }

    @Test fun `weak hints need company`() {
        val engine = TagEngine(TagRules(tags = listOf(TagDef("tt", hints = listOf("alpha", "beta")))))
        fun has(value: String) = engine.tag(text(value)).tags.any { it.name == "tt" }
        assertFalse(has("alpha only"))
        assertTrue(has("alpha and beta"))
    }

    @Test fun `invalid regex is ignored rather than crashing`() {
        val def = TagDef("tt", words = listOf("hello"), rules = listOf(Rule(type = RuleType.REGEX, value = "([unclosed", weight = 5.0)))
        assertTrue(TagEngine(TagRules(tags = listOf(def))).tag(text("hello world")).tags.any { it.name == "tt" })
    }

    @Test fun `imported rules replace same named tags and survive a round trip`() {
        val merged = defaultRules.merge(TagRules(tags = listOf(TagDef("books", words = listOf("tome")), TagDef("knitting", words = listOf("yarn")))))
        assertEquals(defaultRules.tags.size + 1, merged.tags.size)
        assertEquals(listOf("tome"), merged.tags.first { it.tag == "books" }.words)
        assertEquals(merged, TagRules.parse(merged.encode()))
    }

    @Test fun `user typed tags are normalised`() {
        assertEquals(listOf("weekend-trip", "café"), parseTags("#Weekend-Trip, Café"))
        assertEquals("मराठी", normalizeTag("#मराठी"))
    }
}
