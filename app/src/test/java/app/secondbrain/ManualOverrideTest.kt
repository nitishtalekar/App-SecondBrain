package app.secondbrain

import app.secondbrain.classify.CategoryConfig
import app.secondbrain.classify.Classifier
import app.secondbrain.data.db.ItemEntity
import app.secondbrain.data.db.ItemType
import app.secondbrain.ingest.classifyInput
import app.secondbrain.ingest.suggestions
import app.secondbrain.ingest.withClassification
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualOverrideTest {
    private val classifier = Classifier(CategoryConfig.parse(File("src/main/assets/categories.json").readText(Charsets.UTF_8)))

    private fun item(locked: Boolean, category: String?) = ItemEntity(
        type = ItemType.TEXT, body = "Pasta recipe with ingredients for dinner",
        categoryId = category, categoryLocked = locked, createdAt = 0, updatedAt = 0,
    )

    @Test fun `automatic sort assigns a category to an unlocked item`() {
        val sorted = item(locked = false, category = null).let { it.withClassification(classifier.classify(it.classifyInput())) }
        assertEquals("food-recipes", sorted.categoryId)
        assertTrue(sorted.suggestions().isNotEmpty())
    }

    @Test fun `automatic sort never overwrites a manual choice`() {
        val chosen = item(locked = true, category = "travel")
        val after = chosen.withClassification(classifier.classify(chosen.classifyInput()))
        assertEquals("travel", after.categoryId)
        assertTrue(after.categoryLocked)
    }

    @Test fun `a manual move to the inbox is also respected`() {
        val chosen = item(locked = true, category = null)
        val after = chosen.withClassification(classifier.classify(chosen.classifyInput()))
        assertEquals(null, after.categoryId)
    }

    @Test fun `suggestions still refresh for locked items so alternatives stay available`() {
        val chosen = item(locked = true, category = "travel")
        val after = chosen.withClassification(classifier.classify(chosen.classifyInput()))
        assertEquals("food-recipes", after.suggestions().first().categoryId)
    }
}
