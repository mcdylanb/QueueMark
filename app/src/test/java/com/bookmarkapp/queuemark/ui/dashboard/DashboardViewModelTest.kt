package com.bookmarkapp.queuemark.ui.dashboard

import com.bookmarkapp.queuemark.data.remote.UrlMetadata
import com.bookmarkapp.queuemark.testutil.FakeAuthRepository
import com.bookmarkapp.queuemark.testutil.FakeBookmarkRepository
import com.bookmarkapp.queuemark.testutil.FakeGuestSessionStore
import com.bookmarkapp.queuemark.testutil.FakeSyncScheduler
import com.bookmarkapp.queuemark.testutil.FakeUrlMetadataService
import com.bookmarkapp.queuemark.testutil.MainDispatcherRule
import com.bookmarkapp.queuemark.testutil.collectEagerly
import com.bookmarkapp.queuemark.testutil.MutableTimeProvider
import com.bookmarkapp.queuemark.testutil.testBookmark
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
    private val guestStore = FakeGuestSessionStore()
    private val syncScheduler = FakeSyncScheduler()

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        viewModel = DashboardViewModel(
            repository = repository,
            metadataService = metadataService,
            timeProvider = clock,
            authRepository = authRepository,
            guestStore = guestStore,
            syncScheduler = syncScheduler
        )
    }

    @Test
    fun `add bookmark uses scraped title and computed read time`() = runTest {
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DashboardUiAction.OnAddBookmark("https://example.com/a", title = null))

        val saved = repository.rows.value.values.single()
        assertEquals("Scraped Title", saved.title)
        assertEquals(5, saved.estimatedReadTime) // 1000 words / 200 wpm
        assertEquals(42L, saved.createdAt)
        assertEquals("Para one.\n\nPara two.", saved.content)
        assertFalse(saved.isSynced)
    }

    @Test
    fun `scrape failure still saves bookmark with fallback title`() = runTest {
        collectEagerly(viewModel.uiState)
        metadataService.result = Result.failure(RuntimeException("offline"))

        viewModel.onAction(DashboardUiAction.OnAddBookmark("https://example.com/a", title = null))

        val saved = repository.rows.value.values.single()
        assertEquals("https://example.com/a", saved.title)
        assertEquals(1, saved.estimatedReadTime)
    }

    @Test
    fun `manual title beats scraped title`() = runTest {
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DashboardUiAction.OnAddBookmark("https://example.com/a", "My Title"))

        assertEquals("My Title", repository.rows.value.values.single().title)
    }

    @Test
    fun `bare domain gets https scheme, garbage is rejected`() = runTest {
        collectEagerly(viewModel.uiState)

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
        collectEagerly(viewModel.uiState)

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
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DashboardUiAction.OnSearchQueryChange("kotlin"))

        val state = viewModel.uiState.value
        assertTrue(state.isSearching)
        assertEquals(listOf("match"), state.searchResults.map { it.id })
    }

    @Test
    fun `toggle complete flips the bookmark`() = runTest {
        repository.seed(testBookmark("b1"))
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DashboardUiAction.OnToggleComplete("b1"))

        assertTrue(repository.rows.value.getValue("b1").isCompleted)
    }

    // --- account lifecycle (phase 7) ---

    @Test
    fun `logout with no dirty rows proceeds and cleans device-global state`() = runTest {
        repository.seed(testBookmark("b1").copy(isSynced = true))
        guestStore.isGuest = true
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DashboardUiAction.OnLogoutClick)

        assertTrue(viewModel.uiState.value.loggedOut)
        assertTrue(syncScheduler.cancelAllCalled)
        assertTrue(repository.rows.value.isEmpty())
        assertFalse(guestStore.isGuest)
    }

    @Test
    fun `logout with dirty rows warns instead of wiping`() = runTest {
        repository.seed(testBookmark("dirty").copy(isSynced = false))
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DashboardUiAction.OnLogoutClick)

        assertEquals(1, viewModel.uiState.value.logoutWarningCount)
        assertFalse(viewModel.uiState.value.loggedOut)
        assertFalse(repository.rows.value.isEmpty()) // nothing wiped yet

        viewModel.onAction(DashboardUiAction.OnConfirmLogout)
        assertTrue(viewModel.uiState.value.loggedOut)
        assertTrue(repository.rows.value.isEmpty())
    }

    @Test
    fun `dismissing the logout warning keeps the session`() = runTest {
        repository.seed(testBookmark("dirty").copy(isSynced = false))
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DashboardUiAction.OnLogoutClick)
        viewModel.onAction(DashboardUiAction.OnDismissLogoutWarning)

        assertEquals(null, viewModel.uiState.value.logoutWarningCount)
        assertFalse(viewModel.uiState.value.loggedOut)
        assertFalse(repository.rows.value.isEmpty())
    }

    @Test
    fun `local guest without firebase user is labeled Guest with account actions`() = runTest {
        guestStore.isGuest = true
        // recreate so the init collector sees the guest flag
        viewModel = DashboardViewModel(
            repository, metadataService, clock, authRepository, guestStore, syncScheduler
        )
        collectEagerly(viewModel.uiState)

        assertEquals("Guest", viewModel.uiState.value.userLabel)
        assertTrue(viewModel.uiState.value.isAnonymous)
    }

    @Test
    fun `linking with no firebase user routes to sign-up and clears guest flag`() = runTest {
        guestStore.isGuest = true
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DashboardUiAction.OnSubmitLinkAccount("a@b.com", "secret1"))

        assertEquals(1, authRepository.signUpCalls)
        assertEquals(0, authRepository.linkCalls)
        assertFalse(guestStore.isGuest)
    }

    @Test
    fun `linking with an anonymous user uses linkWithEmail`() = runTest {
        authRepository.signInAnonymously()
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DashboardUiAction.OnSubmitLinkAccount("a@b.com", "secret1"))

        assertEquals(0, authRepository.signUpCalls)
        assertEquals(1, authRepository.linkCalls)
    }

    @Test
    fun `guest sign-in to existing account replaces local data on success only`() = runTest {
        guestStore.isGuest = true
        repository.seed(testBookmark("guest-row"))
        collectEagerly(viewModel.uiState)

        authRepository.failEmailSignIn = true
        viewModel.onAction(DashboardUiAction.OnSubmitSignIn("a@b.com", "wrong99"))
        assertFalse(repository.rows.value.isEmpty()) // failed sign-in touches nothing
        assertTrue(guestStore.isGuest)

        authRepository.failEmailSignIn = false
        viewModel.onAction(DashboardUiAction.OnSubmitSignIn("a@b.com", "secret1"))
        assertTrue(repository.rows.value.isEmpty()) // replace semantics
        assertTrue(syncScheduler.cancelAllCalled)
        assertFalse(guestStore.isGuest)
    }

    @Test
    fun `user label falls back to Reader for anonymous`() = runTest {
        collectEagerly(viewModel.uiState)
        assertEquals("Reader", viewModel.uiState.value.userLabel)

        authRepository.signInWithEmail("dylan@school.edu", "secret1")
        assertEquals("dylan", viewModel.uiState.value.userLabel)
    }
}
