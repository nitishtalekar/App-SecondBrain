package app.secondbrain

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.secondbrain.data.decodeTags
import app.secondbrain.data.db.AppDatabase
import app.secondbrain.data.db.MIGRATION_1_2
import app.secondbrain.data.migrated
import app.secondbrain.tagging.TagEngine
import app.secondbrain.tagging.TagKind
import app.secondbrain.tagging.TagRules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test fun foldersAndTagsBecomeTagsWithoutLosingItems() = runBlocking {
        helper.createDatabase(NAME, 1).use { db ->
            db.execSQL("INSERT INTO categories (id, name, color, sortOrder, parentId, rulesJson) VALUES ('travel', 'Travel', '#000000', 0, NULL, '[]')")
            db.execSQL("INSERT INTO categories (id, name, color, sortOrder, parentId, rulesJson) VALUES ('goa', 'Goa Trip', '#000000', 1, 'travel', '[]')")
            fun insert(id: Int, body: String, category: String?, tags: String, status: String, type: String) = db.execSQL(
                "INSERT INTO items (id, type, body, categoryId, categoryLocked, confidence, suggestionsJson, reasons, tags, status, createdAt, updatedAt, searchText) " +
                    "VALUES (?, ?, ?, ?, 1, 0, '[]', '', ?, ?, 100, 100, '')",
                arrayOf<Any?>(id, type, body, category, tags, status),
            )
            insert(1, "Beach shacks to visit", "goa", "winter,friends", "DONE", "TEXT")
            insert(2, "loose note", null, "", "FAILED", "LINK")
        }
        helper.runMigrationsAndValidate(NAME, 2, true, MIGRATION_1_2).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val room = Room.databaseBuilder(context, AppDatabase::class.java, NAME).addMigrations(MIGRATION_1_2).build()
        try {
            val items = room.itemDao().getAll().associateBy { it.id }
            assertEquals(2, items.size)
            val first = items.getValue(1)
            assertEquals("Beach shacks to visit", first.body)
            assertEquals(listOf("winter", "friends", "Goa Trip", "Travel"), first.legacyTags.split('\n'))
            assertTrue(items.getValue(2).needsAnalysis)
            assertEquals("", items.getValue(2).legacyTags)

            val rules = TagRules.parse(context.assets.open("tags.json").bufferedReader().use { it.readText() })
            val tagged = first.migrated(TagEngine(rules), 200)
            val tags = decodeTags(tagged.tagsJson)
            val mine = tags.filter { it.kind == TagKind.MINE }.map { it.name }
            assertTrue(mine.toString(), mine.containsAll(listOf("winter", "friends", "goa-trip", "travel")))
            assertTrue(tags.any { it.name == "beach" })
            assertEquals(TagEngine.VERSION, tagged.taggerVersion)
        } finally {
            room.close()
            context.deleteDatabase(NAME)
        }
    }

    private companion object {
        const val NAME = "migration-test.db"
    }
}
