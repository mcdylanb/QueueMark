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

enum class DetailViewMode { READER, WEB }

data class DetailUiState(
    val bookmark: Bookmark? = null,
    val isLoaded: Boolean = false,
    val viewMode: DetailViewMode = DetailViewMode.WEB,
    val isWebLoadFailed: Boolean = false,
    val showCompletePrompt: Boolean = false,
    val closeScreen: Boolean = false
) {
    val hasReaderContent: Boolean get() = bookmark?.content != null
    val showOfflineEmptyState: Boolean
        get() = viewMode == DetailViewMode.WEB && isWebLoadFailed

    // Stale deep link (deleted bookmark, or a notification from another
    // account): the id resolves to nothing after the DB has answered.
    val notFound: Boolean get() = isLoaded && bookmark == null
}

sealed interface DetailUiAction {
    data object OnExitRequested : DetailUiAction
    data object OnToggleComplete : DetailUiAction
    data object OnConfirmComplete : DetailUiAction
    data object OnDismissPrompt : DetailUiAction
    data object OnToggleViewMode : DetailUiAction

    // Fired when real content becomes visible: Reader composed, or WebView
    // finished loading. Starts the reading session clock.
    data object OnContentShown : DetailUiAction
    data object OnWebLoadFailed : DetailUiAction
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: BookmarkRepository,
    private val timeProvider: TimeProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookmarkId: String =
        checkNotNull(savedStateHandle[QueuemarkDestinations.DETAIL_ARG])

    // Wall-clock reading session (WORKFLOW.md flow 3), started only once real
    // content has been shown — an error page must never count as reading.
    private var sessionStartMillis: Long? = null

    private val modeOverride = MutableStateFlow<DetailViewMode?>(null)
    private val webLoadFailed = MutableStateFlow(false)
    private val prompt = MutableStateFlow(false)
    private val close = MutableStateFlow(false)

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeById(bookmarkId),
        modeOverride,
        webLoadFailed,
        prompt,
        close
    ) { bookmark, override, loadFailed, showPrompt, closeScreen ->
        DetailUiState(
            bookmark = bookmark,
            // combine only emits once every source (incl. the DB read) has;
            // any emission therefore means the lookup finished.
            isLoaded = true,
            viewMode = override
                ?: if (bookmark?.content != null) DetailViewMode.READER else DetailViewMode.WEB,
            isWebLoadFailed = loadFailed,
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

            DetailUiAction.OnToggleViewMode -> {
                val target = if (uiState.value.viewMode == DetailViewMode.READER) {
                    DetailViewMode.WEB
                } else {
                    DetailViewMode.READER
                }
                // Re-entering web mode retries the load from scratch.
                if (target == DetailViewMode.WEB) webLoadFailed.value = false
                modeOverride.value = target
            }

            DetailUiAction.OnContentShown -> {
                if (sessionStartMillis == null) sessionStartMillis = timeProvider.now()
            }

            DetailUiAction.OnWebLoadFailed -> {
                webLoadFailed.value = true
                // Graceful fallback: offline text beats a browser error page.
                if (uiState.value.hasReaderContent && modeOverride.value == null) {
                    modeOverride.value = DetailViewMode.READER
                }
            }
        }
    }

    // ≥80% of the estimated read time spent with content visible counts as "read".
    private fun shouldPromptForCompletion(): Boolean {
        val start = sessionStartMillis ?: return false
        val bookmark = uiState.value.bookmark ?: return false
        if (bookmark.isCompleted) return false
        val minutesSpent = (timeProvider.now() - start) / 60_000.0
        return minutesSpent >= COMPLETION_THRESHOLD * bookmark.estimatedReadTime
    }

    private companion object {
        const val COMPLETION_THRESHOLD = 0.8
    }
}
