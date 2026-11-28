package app.secondbrain

import app.secondbrain.tagging.tagLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class TagLabelTest {

    @Test
    fun tagsReadInTitleCase() {
        assertEquals("Shopping", tagLabel("shopping"))
        assertEquals("Loose Ends", tagLabel("loose ends"))
        assertEquals("From Photo", tagLabel("from-photo"))
        assertEquals("2026", tagLabel("2026"))
        assertEquals("YouTube", tagLabel("youtube"))
    }
}
