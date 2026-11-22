package app.secondbrain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.secondbrain.data.db.AppDatabase
import app.secondbrain.data.db.CategoryEntity
import app.secondbrain.data.db.ItemEntity
import app.secondbrain.data.db.ItemType
import app.secondbrain.data.db.ProcessingStatus
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

    private fun item(text: String, category: String?, status: ProcessingStatus = ProcessingStatus.DONE, at: Long = 1) = ItemEntity(
        type = ItemType.TEXT, body = text, categoryId = category, status = status,
        createdAt = at, updatedAt = at, searchText = text.lowercase(),
    )

    @Test fun inboxExcludesPendingAndCategorised() = runBlocking {
        val dao = db.itemDao()
        dao.insert(item("a", null))
        dao.insert(item("b", "food"))
        dao.insert(item("c", null, ProcessingStatus.PENDING))
        assertEquals(listOf("a"), dao.observeInbox().first().map { it.body })
        assertEquals(listOf("b"), dao.observeByCategory("food").first().map { it.body })
        assertEquals(3, dao.observeAll().first().size)
    }

    @Test fun searchMatchesSubstringsAndEscapesWildcards() = runBlocking {
        val dao = db.itemDao()
        dao.insert(item("fresh croissant at the cafe", "food"))
        dao.insert(item("100% sure", null))
        assertEquals(1, dao.search("croissant").first().size)
        assertEquals(1, dao.search("100!%").first().size)
        assertEquals(0, dao.search("zzz").first().size)
    }

    @Test fun releasingACategoryReturnsItemsToTheInbox() = runBlocking {
        val dao = db.itemDao()
        db.categoryDao().upsert(CategoryEntity("food", "Food", "#C2693F", 0))
        dao.insert(item("a", "food"))
        dao.releaseCategory("food")
        assertEquals(1, dao.observeInbox().first().size)
    }

    @Test fun newestItemsComeFirst() = runBlocking {
        val dao = db.itemDao()
        dao.insert(item("old", null, at = 1))
        dao.insert(item("new", null, at = 2))
        assertEquals(listOf("new", "old"), dao.observeAll().first().map { it.body })
    }
}
