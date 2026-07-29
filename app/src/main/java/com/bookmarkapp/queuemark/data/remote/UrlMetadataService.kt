package com.bookmarkapp.queuemark.data.remote

import com.bookmarkapp.queuemark.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

data class UrlMetadata(
    val title: String?,
    val description: String?,
    val wordCount: Int,
    val content: String? = null
)

interface UrlMetadataService {
    suspend fun fetch(url: String): Result<UrlMetadata>
}

@Singleton
class JsoupUrlMetadataService @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UrlMetadataService {

    override suspend fun fetch(url: String): Result<UrlMetadata> = withContext(ioDispatcher) {
        runCatching {
            val document = Jsoup.connect(url)
                .timeout(TIMEOUT_MS)
                .userAgent(USER_AGENT)
                .get()

            val title = document.selectFirst("meta[property=og:title]")
                ?.attr("content")?.takeIf { it.isNotBlank() }
                ?: document.title().takeIf { it.isNotBlank() }

            val description = document.selectFirst("meta[property=og:description]")
                ?.attr("content")?.takeIf { it.isNotBlank() }
                ?: document.selectFirst("meta[name=description]")
                    ?.attr("content")?.takeIf { it.isNotBlank() }

            val wordCount = document.body()?.text()
                ?.split(WHITESPACE)?.count { it.isNotBlank() }
                ?: 0

            UrlMetadata(
                title = title,
                description = description,
                wordCount = wordCount,
                content = ReadableContentExtractor.extract(document)
            )
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android) Queuemark/1.0"
        val WHITESPACE = Regex("\\s+")
    }
}
