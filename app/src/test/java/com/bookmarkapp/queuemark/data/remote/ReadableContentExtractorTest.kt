package com.bookmarkapp.queuemark.data.remote

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadableContentExtractorTest {

    @Test
    fun `prefers article paragraphs over body noise`() {
        val html = """
            <html><body>
              <p>Cookie banner text</p>
              <article><p>Real first paragraph.</p><p>Real second paragraph.</p></article>
              <footer><p>Footer junk</p></footer>
            </body></html>
        """
        assertEquals(
            "Real first paragraph.\n\nReal second paragraph.",
            ReadableContentExtractor.extract(Jsoup.parse(html))
        )
    }

    @Test
    fun `falls back to main paragraphs`() {
        val html = "<html><body><p>nav</p><main><p>Main text.</p></main></body></html>"
        assertEquals("Main text.", ReadableContentExtractor.extract(Jsoup.parse(html)))
    }

    @Test
    fun `falls back to body paragraphs`() {
        val html = "<html><body><p>Only body text.</p></body></html>"
        assertEquals("Only body text.", ReadableContentExtractor.extract(Jsoup.parse(html)))
    }

    @Test
    fun `returns null when the page has no paragraphs`() {
        val html = "<html><body><div>Just a div</div></body></html>"
        assertNull(ReadableContentExtractor.extract(Jsoup.parse(html)))
    }

    @Test
    fun `filters blank paragraphs`() {
        val html = "<html><body><article><p>Text.</p><p>   </p><p></p></article></body></html>"
        assertEquals("Text.", ReadableContentExtractor.extract(Jsoup.parse(html)))
    }

    @Test
    fun `caps content length at 200k characters`() {
        val paragraph = "<p>${"x".repeat(50_000)}</p>"
        val html = "<html><body><article>${paragraph.repeat(6)}</article></body></html>"
        val content = ReadableContentExtractor.extract(Jsoup.parse(html))!!
        assertEquals(200_000, content.length)
    }

    @Test
    fun `joins paragraphs with blank lines`() {
        val html = "<html><body><article><p>A</p><p>B</p><p>C</p></article></body></html>"
        val content = ReadableContentExtractor.extract(Jsoup.parse(html))!!
        assertTrue(content.split("\n\n") == listOf("A", "B", "C"))
    }
}
