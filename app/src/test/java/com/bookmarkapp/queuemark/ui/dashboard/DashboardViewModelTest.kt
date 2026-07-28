package com.bookmarkapp.queuemark.ui.dashboard

import com.bookmarkapp.queuemark.data.remote.UrlMetadata
import com.bookmarkapp.queuemark.testutil.FakeAuthRepository
import com.bookmarkapp.queuemark.testutil.FakeBookmarkRepository
import com.bookmarkapp.queuemark.testutil.FakeUrlMetadataService
import com.bookmarkapp.queuemark.testutil.MainDispatcherRule
import com.bookmarkapp.queuemark.testutil.MutableTimeProvider
import com.bookmarkapp.queuemark.testutil.testBookmark
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeBookmarkRepository()
    private val metadataService = FakeUrlMetadataService()
    private val clock = MutableTimeProvider(nowMillis = 42L)
    private val authRepository = FakeAuthRepository()

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        viewModel = DashboardViewModel(
            repository = repository,
            metadataService = metadataService,
            timeProvider = clock,
            authRepository = authRepository
        )
    }

    @Test
    fun `add bookmark uses scraped title and computed read time`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onAction(DashboardUiAction.OnAddBookmark("https://example.com/a", title = null))

        val saved = repository.rows.value.values.single()
        assertEquals("Scraped Title", saved.title)
        assertEquals(5, saved.estimatedReadTime) // 1000 words / 200 wpm
        assertEquals(42L, saved.createdAt)
        assertFalse(saved.isSynced)
    }

    @Test
    fun `scrape failure still saves bookmark with fallback title`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        metadataService.result = Result.failure(RuntimeException("offline"))

        viewModel.onAction(DashboardUiAction.OnAddBookmark("https://example.com/a", title = null))

        val saved = repository.rows.value.values.single()
        assertEquals("https://example.com/a", saved.title)
        assertEquals(1, saved.estimatedReadTime)
    }

    @Test
    fun `manual title beats scraped title`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onAction(DashboardUiAction.OnAddBookmark("https://example.com/a", "My Title"))

        assertEquals("My Title", repository.rows.value.values.single().title)
    }

    @Test
    fun `bare domain gets https scheme, garbage is rejected`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onAction(DashboardUiAction.OnAddBookmark("example.com/article", null))
        assertEquals(
            "https://example.com/article",
            repository.rows.value.values.single().url
        )

        repository.rows.value = emptyMap()
        viewModel.onAction(DashboardUiAction.OnAddBookmark("not a url", null))
        assertTrue(repository.rows.value.isEmpty())
        assertNotNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun `state splits unread, completed and quick wins`() = runTest {
        repository.seed(
            testBookmark("short", readTime = 3),
            testBookmark("long", readTime = 20),
            testBookmark("done", readTime = 3, isCompleted = true)
        )
        backgroundScope.launch { viewModel.uiState.collect() }

        val state = viewModel.uiState.value
        assertEquals(setOf("short", "long"), state.unread.map { it.id }.toSet())
        assertEquals(listOf("done"), state.completed.map { it.id })
        assertEquals(listOf("short"), state.quickWins.map { it.id })
    }

    @Test
    fun `search query populates results`() = runTest {
        repository.seed(
            testBookmark("match").copy(title = "Kotlin flows deep dive"),
            testBookmark("other").copy(title = "Swift concurrency")
        )
        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onAction(DashboardUiAction.OnSearchQueryChange("kotlin"))

        val state = viewModel.uiState.value
        assertTrue(state.isSearching)
        assertEquals(listOf("match"), state.searchResults.map { it.id })
    }

    @Test
    fun `toggle complete flips the bookmark`() = runTest {
        repository.seed(testBookmark("b1"))
        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onAction(DashboardUiAction.OnToggleComplete("b1"))

        assertTrue(repository.rows.value.getValue("b1").isCompleted)
    }

    @Test
    fun `user label falls back to Reader for anonymous`() = runTest {
        backgroundScope.launch { viewModel.uiState.collect() }
        assertEquals("Reader", viewModel.uiState.value.userLabel)

        authRepository.signInWithEmail("dylan@school.edu", "secret1")
        assertEquals("dylan", viewModel.uiState.value.userLabel)
    }
}
