package com.bookmarkapp.queuemark.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.bookmarkapp.queuemark.testutil.FakeBookmarkRepository
import com.bookmarkapp.queuemark.testutil.MainDispatcherRule
import com.bookmarkapp.queuemark.testutil.MutableTimeProvider
import com.bookmarkapp.queuemark.testutil.testBookmark
import com.bookmarkapp.queuemark.ui.navigation.QueuemarkDestinations
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
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

    @Test
    fun `exit after 80 percent of read time prompts for completion`() = runTest {
        repository.seed(testBookmark("b1", readTime = 10))
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect() }

        clock.advanceMinutes(8.0) // exactly 80% of 10 minutes

        viewModel.onAction(DetailUiAction.OnExitRequested)

        assertTrue(viewModel.uiState.value.showCompletePrompt)
        assertFalse(viewModel.uiState.value.closeScreen)
    }

    @Test
    fun `exit before threshold closes without prompt`() = runTest {
        repository.seed(testBookmark("b1", readTime = 10))
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect() }

        clock.advanceMinutes(7.9)

        viewModel.onAction(DetailUiAction.OnExitRequested)

        assertFalse(viewModel.uiState.value.showCompletePrompt)
        assertTrue(viewModel.uiState.value.closeScreen)
    }

    @Test
    fun `already completed bookmark never prompts`() = runTest {
        repository.seed(testBookmark("b1", readTime = 10, isCompleted = true))
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect() }

        clock.advanceMinutes(30.0)

        viewModel.onAction(DetailUiAction.OnExitRequested)

        assertFalse(viewModel.uiState.value.showCompletePrompt)
        assertTrue(viewModel.uiState.value.closeScreen)
    }

    @Test
    fun `confirm complete marks bookmark and closes`() = runTest {
        repository.seed(testBookmark("b1", readTime = 1))
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.uiState.collect() }

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
        backgroundScope.launch { viewModel.uiState.collect() }

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
        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onAction(DetailUiAction.OnToggleComplete)
        assertTrue(repository.rows.value.getValue("b1").isCompleted)

        viewModel.onAction(DetailUiAction.OnToggleComplete)
        assertFalse(repository.rows.value.getValue("b1").isCompleted)
    }
}
