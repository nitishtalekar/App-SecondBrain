package app.secondbrain

import app.secondbrain.ingest.LinkParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkParserTest {
    @Test fun `bare url is a link`() {
        val p = LinkParser.parse("https://example.com/a?b=1")
        assertEquals("https://example.com/a?b=1", p.url)
        assertEquals("", p.note)
    }

    @Test fun `www without scheme gets https`() {
        assertEquals("https://www.example.com", LinkParser.parse("www.example.com").url)
    }

    @Test fun `share sheet title plus url is a link with a note`() {
        val p = LinkParser.parse("Great pasta recipe https://cook.example.com/pasta")
        assertEquals("https://cook.example.com/pasta", p.url)
        assertEquals("Great pasta recipe", p.note)
    }

    @Test fun `trailing punctuation is not part of the url`() {
        assertEquals("https://example.com/x", LinkParser.parse("see https://example.com/x.").url)
    }

    @Test fun `long prose that mentions a url stays text`() {
        val prose = "I was thinking about how we organise things " + "and so on ".repeat(20) + "https://example.com"
        assertNull(LinkParser.parse(prose).url)
    }

    @Test fun `plain text has no url`() {
        assertNull(LinkParser.parse("buy milk").url)
    }
}
