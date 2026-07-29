package com.bookmarkapp.queuemark.data.remote

import org.jsoup.nodes.Document
import org.jsoup.select.Elements

// Pulls readable article text out of an already-parsed page for offline
// reading. Bad or empty extraction returns null, which downstream means
// "no reader mode, fall back to the WebView" — same behavior as before
// this feature existed.
object ReadableContentExtractor {
    private const val MAX_CONTENT_CHARS = 200_000

    // Fallback chain: <article> paragraphs -> <main> paragraphs -> all body
    // paragraphs. Paragraphs are stored joined by blank lines; the reader
    // screen splits on the same separator.
    fun extract(document: Document): String? {
        val paragraphs = document.select("article p")
            .ifEmpty { document.select("main p") }
            .ifEmpty { document.body()?.select("p") ?: Elements() }

        val text = paragraphs
            .map { it.text().trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n\n")

        return text.takeIf { it.isNotBlank() }?.take(MAX_CONTENT_CHARS)
    }
}
