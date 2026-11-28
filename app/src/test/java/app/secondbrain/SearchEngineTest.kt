package app.secondbrain

import app.secondbrain.search.SearchDoc
import app.secondbrain.search.SearchEngine
import app.secondbrain.search.SearchParams
import app.secondbrain.search.StatusFilter
import app.secondbrain.tagging.ItemAction
import app.secondbrain.tagging.ItemType
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineTest {

    private val now = Calendar.getInstance().apply { set(2026, Calendar.MARCH, 15, 12, 0) }.timeInMillis
    private val hour = 3_600_000L

    private val docs = listOf(
        entry(1, "Carbonara with pecorino", listOf("recipes", "food"), createdAt = now - hour),
        entry(2, "Piranesi", listOf("books"), action = ItemAction.READ, createdAt = now - 24 * hour),
        entry(3, "Buy oat milk", listOf("groceries", "food"), action = ItemAction.BUY, completedAt = now, createdAt = now - 90 * 24 * hour),
        entry(4, "", listOf("coffee"), type = ItemType.IMAGE, ocrText = "Welcome to the Blue Door Café, open daily", createdAt = now - 90 * 24 * hour),
        entry(5, "party on saturday", createdAt = now - 90 * 24 * hour),
    ).map(::SearchDoc)

    private fun ids(query: String = "", params: SearchParams = SearchParams(query)) =
        SearchEngine.search(docs, params, now).map { it.entry.id }

    @Test fun `empty search returns everything newest first`() {
        assertEquals(listOf(1L, 2L), ids().take(2))
        assertEquals(5, ids().size)
    }

    @Test fun `words match body tags and photo text`() {
        assertEquals(listOf(1L), ids("pecorino"))
        assertEquals(setOf(1L, 3L), ids("food").toSet())
        assertEquals(listOf(4L), ids("blue door"))
    }

    @Test fun `all words must match`() {
        assertEquals(listOf(3L), ids("food milk"))
        assertTrue(ids("food zebra").isEmpty())
    }

    @Test fun `accents plurals and typos are forgiven`() {
        assertEquals(setOf(4L), ids("cafe").toSet())
        assertEquals(listOf(1L), ids("recipe"))
        assertEquals(listOf(1L), ids("carbonaro"))
        assertEquals(listOf(2L), ids("pirnaesi"))
    }

    @Test fun `hash searches tags only`() {
        assertEquals(setOf(1L, 3L), ids("#food").toSet())
        assertTrue(ids("#pecorino").isEmpty())
    }

    @Test fun `exclusion does not hide words that merely contain it`() {
        assertEquals(listOf(1L), ids("food -milk"))
        assertTrue(5L in ids("-art"))
    }

    @Test fun `quoted phrase must appear together`() {
        assertEquals(listOf(4L), ids("\"blue door\""))
        assertTrue(ids("\"door blue\"").isEmpty())
    }

    @Test fun `state is searchable by word and by filter`() {
        assertEquals(listOf(2L), ids("to read"))
        assertEquals(listOf(3L), ids("done"))
        assertEquals(listOf(2L), ids(params = SearchParams(status = StatusFilter.READ)))
        assertEquals(listOf(3L), ids(params = SearchParams(status = StatusFilter.DONE)))
        // Completed things are no longer waiting.
        assertTrue(ids(params = SearchParams(status = StatusFilter.BUY)).isEmpty())
    }

    @Test fun `type and tag filters combine`() {
        assertEquals(listOf(4L), ids(params = SearchParams(type = ItemType.IMAGE)))
        assertEquals(listOf(3L), ids(params = SearchParams(tags = setOf("food", "groceries"))))
    }

    @Test fun `dates are searchable`() {
        assertEquals(listOf(1L), ids("today"))
        assertEquals(listOf(2L), ids("yesterday"))
        assertEquals(setOf(1L, 2L), ids("march").toSet())
    }

    @Test fun `tag hits rank above body hits`() {
        val ranked = SearchEngine.search(
            listOf(entry(1, "coffee beans", createdAt = 9), entry(2, "morning", listOf("coffee"), createdAt = 1)).map(::SearchDoc),
            SearchParams("coffee"),
        )
        assertEquals(2L, ranked.first().entry.id)
    }

    @Test fun `snippet shows where a hidden match was found`() {
        val doc = SearchDoc(entry(9, "Cafe notes\nThey are open daily until late"))
        val hit = SearchEngine.search(listOf(doc), SearchParams("daily"), now).single()
        assertNotNull(hit.snippet)
        assertTrue(hit.snippet!!.contains("daily"))
    }

    @Test fun `suggestions complete the word being typed`() {
        val docs = listOf(
            SearchDoc(entry(1, "pasta", tags = listOf("food", "recipes"))),
            SearchDoc(entry(2, "flight", tags = listOf("travel", "seafood"))),
        )
        fun suggest(query: String, tags: Set<String> = emptySet()) =
            SearchEngine.suggestTags(docs, SearchParams(query = query, tags = tags)).map { it.name }

        assertEquals(listOf("food", "seafood"), suggest("foo"))
        assertEquals(listOf("food"), suggest("pasta #fo"))
        assertEquals(emptyList<String>(), suggest("foo", tags = setOf("travel")) - "seafood")
        assertEquals(emptyList<String>(), suggest("food "))
        assertEquals(emptyList<String>(), suggest("-foo"))
    }

    @Test fun `tag counts group and exclude chosen tags`() {
        val counts = SearchEngine.tagCounts(docs.map { it.entry }, exclude = setOf("books"))
        assertEquals("food", counts.first().name)
        assertEquals(2, counts.first().count)
        assertTrue(counts.none { it.name == "books" })
    }
}
