package app.secondbrain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.secondbrain.data.db.AppDatabase
import app.secondbrain.data.db.ItemEntity
import app.secondbrain.tagging.ItemAction
import app.secondbrain.tagging.ItemType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).build()
    }

    @After fun tearDown() = db.close()

    private fun item(text: String, at: Long = 1, image: String? = null, pending: Boolean = false, version: Int = 1) = ItemEntity(
        type = ItemType.TEXT, body = text, imagePath = image, needsAnalysis = pending, taggerVersion = version,
        createdAt = at, updatedAt = at,
    )

    @Test fun newestItemsComeFirst() = runBlocking {
        val dao = db.itemDao()
        dao.insertAll(listOf(item("old", at = 1), item("new", at = 2)))
        assertEquals(listOf("new", "old"), dao.observeAll().first().map { it.body })
    }

    @Test fun actionAndCompletionRoundTrip() = runBlocking {
        val dao = db.itemDao()
        val id = dao.insert(item("milk").copy(action = ItemAction.BUY, completedAt = 5))
        val saved = dao.get(id)!!
        assertEquals(ItemAction.BUY, saved.action)
        assertEquals(5L, saved.completedAt)
    }

    @Test fun housekeepingQueries() = runBlocking {
        val dao = db.itemDao()
        val ids = dao.insertAll(listOf(item("a", pending = true), item("b", version = 0), item("c", image = "/p.jpg")))
        assertEquals(listOf("a"), dao.getNeedingAnalysis().map { it.body })
        assertEquals(listOf(ids[1]), dao.getTaggedBefore(1))
        assertEquals(1, dao.countImageUses("/p.jpg"))
        dao.delete(ids[2])
        assertEquals(0, dao.countImageUses("/p.jpg"))
    }
}
