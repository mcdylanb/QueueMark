package com.bookmarkapp.queuemark.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookmarkapp.queuemark.data.repository.BookmarkRepository
import com.bookmarkapp.queuemark.domain.TimeProvider
import com.bookmarkapp.queuemark.domain.model.Bookmark
import com.bookmarkapp.queuemark.ui.navigation.QueuemarkDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val bookmark: Bookmark? = null,
    val showCompletePrompt: Boolean = false,
    val closeScreen: Boolean = false
)

sealed interface DetailUiAction {
    data object OnExitRequested : DetailUiAction
    data object OnToggleComplete : DetailUiAction
    data object OnConfirmComplete : DetailUiAction
    data object OnDismissPrompt : DetailUiAction
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: BookmarkRepository,
    private val timeProvider: TimeProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookmarkId: String =
        checkNotNull(savedStateHandle[QueuemarkDestinations.DETAIL_ARG])

    // Wall-clock reading session, started when the screen opens (WORKFLOW.md flow 3).
    private val sessionStartMillis: Long = timeProvider.now()

    private val prompt = MutableStateFlow(false)
    private val close = MutableStateFlow(false)

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeById(bookmarkId),
        prompt,
        close
    ) { bookmark, showPrompt, closeScreen ->
        DetailUiState(
            bookmark = bookmark,
            showCompletePrompt = showPrompt,
            closeScreen = closeScreen
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun onAction(action: DetailUiAction) {
        when (action) {
            DetailUiAction.OnExitRequested -> {
                if (shouldPromptForCompletion()) {
                    prompt.value = true
                } else {
                    close.value = true
                }
            }

            DetailUiAction.OnToggleComplete -> viewModelScope.launch {
                val bookmark = uiState.value.bookmark ?: return@launch
                repository.setCompleted(bookmarkId, !bookmark.isCompleted)
            }

            DetailUiAction.OnConfirmComplete -> viewModelScope.launch {
                repository.setCompleted(bookmarkId, true)
                prompt.update { false }
                close.value = true
            }

            DetailUiAction.OnDismissPrompt -> {
                prompt.value = false
                close.value = true
            }
        }
    }

    // ≥80% of the estimated read time spent on the page counts as "read".
    private fun shouldPromptForCompletion(): Boolean {
        val bookmark = uiState.value.bookmark ?: return false
        if (bookmark.isCompleted) return false
        val minutesSpent = (timeProvider.now() - sessionStartMillis) / 60_000.0
        return minutesSpent >= COMPLETION_THRESHOLD * bookmark.estimatedReadTime
    }

    private companion object {
        const val COMPLETION_THRESHOLD = 0.8
    }
}
