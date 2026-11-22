package app.secondbrain

import app.secondbrain.classify.CategoryConfig
import app.secondbrain.classify.Classifier
import app.secondbrain.classify.ClassifyInput
import app.secondbrain.classify.Field
import app.secondbrain.classify.CategoryDef
import app.secondbrain.classify.Rule
import app.secondbrain.classify.RuleType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassifierTest {

    private val defaults = CategoryConfig.parse(
        File("src/main/assets/categories.json").readText(Charsets.UTF_8),
    )
    private val classifier = Classifier(defaults)

    private fun classify(input: ClassifyInput) = classifier.classify(input)

    @Test fun `default config parses and has the starter categories`() {
        val ids = defaults.categories.map { it.id }
        assertTrue(ids.containsAll(listOf("books", "food", "travel", "entertainment", "ideas", "quotes", "notes")))
    }

    @Test fun `recipe text goes to food`() {
        val r = classify(ClassifyInput(text = "Try this pasta carbonara recipe, ingredients are eggs and pecorino"))
        assertEquals("food", r.categoryId)
        assertTrue(r.reasons.isNotEmpty())
    }

    @Test fun `reminder goes to notes`() {
        assertEquals("notes", classify(ClassifyInput(text = "Remember to call mom tomorrow")).categoryId)
    }

    @Test fun `quoted attribution goes to quotes`() {
        val text = "“The only way out is through.” — Robert Frost"
        val r = classify(ClassifyInput(text = text))
        assertEquals(r.ranked.toString() + r.confidence, "quotes", r.categoryId)
    }

    @Test fun `idea phrasing goes to ideas`() {
        assertEquals("ideas", classify(ClassifyInput(text = "What if there was a startup idea for sharing tools between neighbours")).categoryId)
    }

    @Test fun `domain rule categorises a bare link`() {
        val r = classify(ClassifyInput(url = "https://www.goodreads.com/book/show/12345"))
        assertEquals("books", r.categoryId)
    }

    @Test fun `page metadata is used for links`() {
        val r = classify(ClassifyInput(url = "https://example.com/x", pageTitle = "10 things to do in Lisbon", pageDescription = "A travel itinerary for a long weekend"))
        assertEquals("travel", r.categoryId)
    }

    @Test fun `image labels and ocr drive image categorisation`() {
        val r = classify(ClassifyInput(imageLabels = listOf("Food", "Dish", "Cuisine"), ocrText = "Menu Pasta Pizza"))
        assertEquals("food", r.categoryId)
    }

    @Test fun `unrecognisable text goes to the inbox but is still ranked`() {
        val r = classify(ClassifyInput(text = "xkcd qwerty zzz"))
        assertNull(r.categoryId)
    }

    @Test fun `a single weak hit is not confident enough`() {
        val r = classify(ClassifyInput(text = "ok see you at six"))
        assertNull(r.categoryId)
    }

    @Test fun `whole word matching avoids substring false positives`() {
        // "art" must not match inside "party"; "cook" must not match "cookie" only when exact would be required
        val config = CategoryConfig(categories = listOf(CategoryDef("a", "A", rules = listOf(Rule(value = "art", weight = 5.0)))))
        assertNull(Classifier(config).classify(ClassifyInput(text = "a party apart")).categoryId)
        assertEquals("a", Classifier(config).classify(ClassifyInput(text = "modern art museum")).categoryId)
        assertEquals("a", Classifier(config).classify(ClassifyInput(text = "two arts")).categoryId)
    }

    @Test fun `long keywords match as a prefix`() {
        val config = CategoryConfig(categories = listOf(CategoryDef("a", "A", rules = listOf(Rule(value = "recipe", weight = 5.0)))))
        assertEquals("a", Classifier(config).classify(ClassifyInput(text = "recipes for dinner")).categoryId)
    }

    @Test fun `field restriction is respected`() {
        val config = CategoryConfig(categories = listOf(CategoryDef("a", "A", rules = listOf(Rule(value = "lisbon", weight = 5.0, field = Field.URL)))))
        assertNull(Classifier(config).classify(ClassifyInput(text = "visiting lisbon")).categoryId)
        assertEquals("a", Classifier(config).classify(ClassifyInput(url = "https://x.com/lisbon-guide")).categoryId)
    }

    @Test fun `invalid regex is ignored rather than crashing`() {
        val config = CategoryConfig(categories = listOf(CategoryDef("a", "A", rules = listOf(
            Rule(type = RuleType.REGEX, value = "([unclosed", weight = 5.0),
            Rule(value = "hello", weight = 5.0),
        ))))
        assertEquals("a", Classifier(config).classify(ClassifyInput(text = "hello world")).categoryId)
    }

    @Test fun `alternatives are ranked best first`() {
        val r = classify(ClassifyInput(text = "Movie night idea: watch the trailer for that new film"))
        assertNotNull(r.ranked.firstOrNull())
        assertEquals(r.categoryId, r.ranked.first().categoryId)
        assertTrue(r.ranked.zipWithNext().all { (a, b) -> a.score >= b.score })
    }

    @Test fun `config round trips through json`() {
        val again = CategoryConfig.parse(defaults.encode())
        assertEquals(defaults.categories.size, again.categories.size)
        assertEquals(defaults.categories.first().rules.size, again.categories.first().rules.size)
    }

    @Test fun `host extraction`() {
        assertEquals("goodreads.com", Classifier.hostOf("https://www.goodreads.com/book/1?x=1"))
        assertEquals("example.org", Classifier.hostOf("http://user@example.org:8080/path"))
        assertNull(Classifier.hostOf("not a url"))
    }
}
