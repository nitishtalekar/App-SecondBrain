package app.secondbrain

import app.secondbrain.data.decodeTags
import app.secondbrain.data.removedTagSet
import app.secondbrain.data.retaggedForNewContent
import app.secondbrain.data.withAutoTags
import app.secondbrain.data.withUserTags
import app.secondbrain.data.withoutTag
import app.secondbrain.ingest.DraftAssembler
import app.secondbrain.ingest.ImageAnalysis
import app.secondbrain.tagging.ItemAction
import app.secondbrain.tagging.ItemType
import app.secondbrain.tagging.Tag
import app.secondbrain.tagging.TagKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftTest {

    private var keys = 0

    private fun assemble(text: String = "", image: String? = null, analysis: ImageAnalysis? = null) =
        DraftAssembler.assemble(text, image, analysis, null, defaultEngine, now = 1_000, newKey = { "k${keys++}" })

    private val menu = ImageAnalysis("Blue Door Cafe\nFresh coffee and breakfast served daily", listOf("Menu", "Food"), failed = false)

    @Test fun `a photo with text becomes a photo post and a text post`() {
        val drafts = assemble("from our walk", "/x.jpg", menu)
        assertEquals(listOf(ItemType.IMAGE, ItemType.TEXT), drafts.map { it.type })
        val (photo, words) = drafts
        assertEquals("from our walk", photo.body)
        assertNull(photo.ocrText)
        assertNull(words.imagePath)
        assertTrue(words.scanned)
        assertEquals(menu.text, words.body)
        assertNotNull(photo.groupId)
        assertEquals(photo.groupId, words.groupId)
        assertTrue(words.tags.any { it.name == "coffee" })
        assertTrue(photo.tags.any { it.name == "food" })
        // The picture is findable by what its text is about.
        assertTrue(photo.tags.any { it.name == "coffee" })
        assertTrue(photo.retagged(defaultEngine).tags.any { it.name == "coffee" })
    }

    @Test fun `a stray word stays with the photo`() {
        val drafts = assemble(image = "/x.jpg", analysis = ImageAnalysis("NIKE", emptyList(), failed = false))
        assertEquals(1, drafts.size)
        assertEquals("NIKE", drafts.single().ocrText)
        assertNull(drafts.single().groupId)
    }

    @Test fun `an unreadable photo is still a post and is retried`() {
        val drafts = assemble(image = "/x.jpg", analysis = null)
        assertTrue(drafts.single().needsAnalysis)
        assertTrue(drafts.single().canSave)
    }

    @Test fun `a link that could not be fetched is flagged`() {
        val draft = assemble("https://example.com/a").single()
        assertEquals(ItemType.LINK, draft.type)
        assertTrue(draft.needsAnalysis)
    }

    @Test fun `nothing written makes no post`() {
        assertTrue(assemble("   ").isEmpty())
    }

    @Test fun `switched off tags are not saved and are remembered`() {
        val draft = assemble("pasta recipe for dinner").single()
        val item = draft.toggleTag("recipes").toItem(2_000)
        assertFalse(decodeTags(item.tagsJson).any { it.name == "recipes" })
        assertTrue("recipes" in item.removedTagSet())
        // Switching it back on undoes both.
        val again = draft.toggleTag("recipes").toggleTag("recipes").toItem(2_000)
        assertTrue(decodeTags(again.tagsJson).any { it.name == "recipes" })
        assertTrue(again.removedTagSet().isEmpty())
    }

    @Test fun `typed tags and chosen action survive re-tagging`() {
        val draft = assemble("buy milk").single()
        assertEquals(ItemAction.BUY, draft.action)
        val edited = draft.addTags(listOf("Home")).choose(null).copy(body = "buy oat milk").retagged(defaultEngine)
        assertNull(edited.action)
        assertTrue(edited.tags.any { it.name == "home" && it.kind == TagKind.MINE })
        assertTrue(edited.tags.any { it.name == "oat" })
    }

    @Test fun `already done is saved as completed`() {
        val draft = assemble("read Dune").single().copy(done = true)
        assertEquals(2_000L, draft.toItem(2_000).completedAt)
        assertNull(draft.choose(null).toItem(2_000).completedAt)
    }

    @Test fun `automatic tags never revive a removed tag`() {
        val item = assemble("pasta recipe").single().toItem(1).withoutTag("food")
        val after = item.withAutoTags(listOf(Tag("food", TagKind.TOPIC), Tag("new-one", TagKind.TOPIC)))
        val names = decodeTags(after.tagsJson).map { it.name }
        assertFalse("food" in names)
        assertTrue("new-one" in names)
        // Adding it by hand overrides the removal.
        assertTrue(decodeTags(after.withUserTags(listOf("food")).tagsJson).any { it.name == "food" })
    }

    @Test fun `editing content replaces automatic tags but keeps the user's`() {
        val item = assemble("pasta recipe").single().toItem(1).withUserTags(listOf("favourite"))
        val after = item.retaggedForNewContent(listOf(Tag("travel", TagKind.TOPIC)))
        assertEquals(setOf("favourite", "travel"), decodeTags(after.tagsJson).map { it.name }.toSet())
    }
}
