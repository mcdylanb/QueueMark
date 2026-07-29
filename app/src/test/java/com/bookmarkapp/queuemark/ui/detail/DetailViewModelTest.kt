package com.bookmarkapp.queuemark.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.bookmarkapp.queuemark.testutil.FakeBookmarkRepository
import com.bookmarkapp.queuemark.testutil.MainDispatcherRule
import com.bookmarkapp.queuemark.testutil.collectEagerly
import com.bookmarkapp.queuemark.testutil.MutableTimeProvider
import com.bookmarkapp.queuemark.testutil.testBookmark
import com.bookmarkapp.queuemark.ui.navigation.QueuemarkDestinations
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeBookmarkRepository()
    private val clock = MutableTimeProvider(nowMillis = 0L)

    private fun createViewModel(bookmarkId: String = "b1") = DetailViewModel(
        repository = repository,
        timeProvider = clock,
        savedStateHandle = SavedStateHandle(mapOf(QueuemarkDestinations.DETAIL_ARG to bookmarkId))
    )

    // --- reading-session prompt ---

    @Test
    fun `exit after 80 percent of read time prompts for completion`() = runTest {
        repository.seed(testBookmark("b1", readTime = 10))
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DetailUiAction.OnContentShown)
        clock.advanceMinutes(8.0) // exactly 80% of 10 minutes

        viewModel.onAction(DetailUiAction.OnExitRequested)

        assertTrue(viewModel.uiState.value.showCompletePrompt)
        assertFalse(viewModel.uiState.value.closeScreen)
    }

    @Test
    fun `exit before threshold closes without prompt`() = runTest {
        repository.seed(testBookmark("b1", readTime = 10))
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DetailUiAction.OnContentShown)
        clock.advanceMinutes(7.9)

        viewModel.onAction(DetailUiAction.OnExitRequested)

        assertFalse(viewModel.uiState.value.showCompletePrompt)
        assertTrue(viewModel.uiState.value.closeScreen)
    }

    @Test
    fun `already completed bookmark never prompts`() = runTest {
        repository.seed(testBookmark("b1", readTime = 10, isCompleted = true))
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DetailUiAction.OnContentShown)
        clock.advanceMinutes(30.0)

        viewModel.onAction(DetailUiAction.OnExitRequested)

        assertFalse(viewModel.uiState.value.showCompletePrompt)
        assertTrue(viewModel.uiState.value.closeScreen)
    }

    @Test
    fun `no prompt when content never showed, regardless of elapsed time`() = runTest {
        repository.seed(testBookmark("b1", readTime = 1))
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        clock.advanceMinutes(30.0) // stared at an error page for half an hour

        viewModel.onAction(DetailUiAction.OnExitRequested)

        assertFalse(viewModel.uiState.value.showCompletePrompt)
        assertTrue(viewModel.uiState.value.closeScreen)
    }

    @Test
    fun `session clock starts at first content shown, not screen open`() = runTest {
        repository.seed(testBookmark("b1", readTime = 10))
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        clock.advanceMinutes(60.0) // long delay before anything loads
        viewModel.onAction(DetailUiAction.OnContentShown)
        clock.advanceMinutes(7.9) // under the threshold measured from content-shown

        viewModel.onAction(DetailUiAction.OnExitRequested)

        assertFalse(viewModel.uiState.value.showCompletePrompt)
    }

    @Test
    fun `confirm complete marks bookmark and closes`() = runTest {
        repository.seed(testBookmark("b1", readTime = 1))
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DetailUiAction.OnContentShown)
        clock.advanceMinutes(1.0)
        viewModel.onAction(DetailUiAction.OnExitRequested)
        viewModel.onAction(DetailUiAction.OnConfirmComplete)

        assertTrue(repository.rows.value.getValue("b1").isCompleted)
        assertTrue(viewModel.uiState.value.closeScreen)
    }

    @Test
    fun `dismissing the prompt closes without completing`() = runTest {
        repository.seed(testBookmark("b1", readTime = 1))
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DetailUiAction.OnContentShown)
        clock.advanceMinutes(1.0)
        viewModel.onAction(DetailUiAction.OnExitRequested)
        viewModel.onAction(DetailUiAction.OnDismissPrompt)

        assertFalse(repository.rows.value.getValue("b1").isCompleted)
        assertTrue(viewModel.uiState.value.closeScreen)
    }

    @Test
    fun `toggle complete from the top bar works both ways`() = runTest {
        repository.seed(testBookmark("b1"))
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DetailUiAction.OnToggleComplete)
        assertTrue(repository.rows.value.getValue("b1").isCompleted)

        viewModel.onAction(DetailUiAction.OnToggleComplete)
        assertFalse(repository.rows.value.getValue("b1").isCompleted)
    }

    // --- view modes ---

    @Test
    fun `defaults to reader when content exists`() = runTest {
        repository.seed(testBookmark("b1", content = "Saved text."))
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        assertEquals(DetailViewMode.READER, viewModel.uiState.value.viewMode)
        assertTrue(viewModel.uiState.value.hasReaderContent)
    }

    @Test
    fun `defaults to web when content is null`() = runTest {
        repository.seed(testBookmark("b1"))
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        assertEquals(DetailViewMode.WEB, viewModel.uiState.value.viewMode)
        assertFalse(viewModel.uiState.value.hasReaderContent)
    }

    @Test
    fun `toggle switches modes both ways`() = runTest {
        repository.seed(testBookmark("b1", content = "Saved text."))
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DetailUiAction.OnToggleViewMode)
        assertEquals(DetailViewMode.WEB, viewModel.uiState.value.viewMode)

        viewModel.onAction(DetailUiAction.OnToggleViewMode)
        assertEquals(DetailViewMode.READER, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `web load failure without content shows offline empty state`() = runTest {
        repository.seed(testBookmark("b1"))
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DetailUiAction.OnWebLoadFailed)

        assertTrue(viewModel.uiState.value.showOfflineEmptyState)
    }

    @Test
    fun `manual web mode failure with content shows offline state with reader escape`() = runTest {
        repository.seed(testBookmark("b1", content = "Saved text."))
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DetailUiAction.OnToggleViewMode) // user explicitly chose WEB
        viewModel.onAction(DetailUiAction.OnWebLoadFailed)

        assertEquals(DetailViewMode.WEB, viewModel.uiState.value.viewMode)
        assertTrue(viewModel.uiState.value.showOfflineEmptyState)
        assertTrue(viewModel.uiState.value.hasReaderContent)
    }

    @Test
    fun `re-entering web mode retries by clearing the failure flag`() = runTest {
        repository.seed(testBookmark("b1", content = "Saved text."))
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DetailUiAction.OnToggleViewMode) // -> WEB
        viewModel.onAction(DetailUiAction.OnWebLoadFailed)
        viewModel.onAction(DetailUiAction.OnToggleViewMode) // -> READER
        viewModel.onAction(DetailUiAction.OnToggleViewMode) // -> WEB again

        assertFalse(viewModel.uiState.value.isWebLoadFailed)
        assertFalse(viewModel.uiState.value.showOfflineEmptyState)
    }

    @Test
    fun `web load failure with content auto-switches to reader`() = runTest {
        repository.seed(testBookmark("b1", content = "Saved text."))
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.onAction(DetailUiAction.OnToggleViewMode) // user chose WEB
        // simulate override cleared state: a fresh open in web-default with content
        val webDefault = createViewModel()
        collectEagerly(webDefault.uiState)

        webDefault.onAction(DetailUiAction.OnWebLoadFailed)

        assertEquals(DetailViewMode.READER, webDefault.uiState.value.viewMode)
        assertFalse(webDefault.uiState.value.showOfflineEmptyState)
    }
}
