package app.secondbrain

import app.secondbrain.search.FeedOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FeedOrderTest {
    private val ids = (1L..50L).toList()

    private fun shuffle(list: List<Long>, seed: Long) = FeedOrder.shuffle(list, seed) { it }

    @Test
    fun keepsEveryItemInANewOrder() {
        val shuffled = shuffle(ids, 7)
        assertEquals(ids, shuffled.sorted())
        assertNotEquals(ids, shuffled)
    }

    @Test
    fun sameSeedSameOrderWhateverTheInputOrder() {
        assertEquals(shuffle(ids, 7), shuffle(ids.reversed(), 7))
    }

    @Test
    fun differentSeedsDiffer() {
        assertNotEquals(shuffle(ids, 7), shuffle(ids, 8))
    }

    @Test
    fun filteringAndAddingLeaveTheRestInPlace() {
        val all = shuffle(ids, 7)
        val some = ids.filter { it % 2 == 0L }
        assertEquals(all.filter { it in some }, shuffle(some, 7))
        assertEquals(all, shuffle(ids + 51L, 7).filter { it != 51L })
    }
}
