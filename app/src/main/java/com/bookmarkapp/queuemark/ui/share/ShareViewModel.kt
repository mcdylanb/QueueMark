package com.bookmarkapp.queuemark.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookmarkapp.queuemark.data.remote.SyncScheduler
import com.bookmarkapp.queuemark.data.remote.UrlMetadataService
import com.bookmarkapp.queuemark.data.repository.BookmarkRepository
import com.bookmarkapp.queuemark.domain.ReadTimeCalculator
import com.bookmarkapp.queuemark.domain.ReminderPreset
import com.bookmarkapp.queuemark.domain.ReminderTimeCalculator
import com.bookmarkapp.queuemark.domain.TimeProvider
import com.bookmarkapp.queuemark.domain.model.Bookmark
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShareUiState(
    val scrapedTitle: String? = null,
    val hasValidUrl: Boolean = true,
    val selectedPreset: ReminderPreset? = null,
    val customPicked: Boolean = false,
    val showCustomPicker: Boolean = false,
    val isFinished: Boolean = false
)

sealed interface ShareUiAction {
    data class OnPresetSelected(val preset: ReminderPreset) : ShareUiAction
    data object OnCustomClicked : ShareUiAction
    data class OnCustomPicked(val epochMillis: Long) : ShareUiAction
    data object OnCustomDismissed : ShareUiAction
    data object OnDone : ShareUiAction
}

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val repository: BookmarkRepository,
    private val metadataService: UrlMetadataService,
    private val syncScheduler: SyncScheduler,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    private var bookmarkId: String? = null

    // Sheet must show instantly (WORKFLOW.md flow 1): the id exists up front,
    // scrape + insert run behind it, reminders attach to the id.
    fun onSharedText(sharedText: String?) {
        if (bookmarkId != null) return // config-change re-delivery
        val url = extractUrl(sharedText)
        if (url == null) {
            _uiState.update { it.copy(hasValidUrl = false) }
            return
        }
        val id = UUID.randomUUID().toString()
        bookmarkId = id

        viewModelScope.launch {
            val metadata = metadataService.fetch(url).getOrNull()
            metadata?.title?.let { title ->
                _uiState.update { it.copy(scrapedTitle = title) }
            }
            repository.add(
                Bookmark(
                    id = id,
                    url = url,
                    title = metadata?.title ?: url,
                    description = metadata?.description,
                    content = metadata?.content,
                    estimatedReadTime = ReadTimeCalculator.estimateMinutes(
                        metadata?.wordCount ?: 0
                    ),
                    createdAt = timeProvider.now(),
                    reminderTime = null,
                    isCompleted = false,
                    completedAt = null,
                    isSynced = false
                )
            )
        }
    }

    fun onAction(action: ShareUiAction) {
        when (action) {
            is ShareUiAction.OnPresetSelected -> {
                val trigger = ReminderTimeCalculator.forPreset(action.preset, timeProvider.now())
                setReminder(trigger)
                _uiState.update { it.copy(selectedPreset = action.preset, customPicked = false) }
            }

            ShareUiAction.OnCustomClicked ->
                _uiState.update { it.copy(showCustomPicker = true) }

            is ShareUiAction.OnCustomPicked -> {
                setReminder(action.epochMillis)
                _uiState.update {
                    it.copy(selectedPreset = null, customPicked = true, showCustomPicker = false)
                }
            }

            ShareUiAction.OnCustomDismissed ->
                _uiState.update { it.copy(showCustomPicker = false) }

            ShareUiAction.OnDone ->
                _uiState.update { it.copy(isFinished = true) }
        }
    }

    private fun setReminder(triggerAtMillis: Long) {
        val id = bookmarkId ?: return
        viewModelScope.launch {
            repository.setReminder(id, triggerAtMillis)
            syncScheduler.scheduleReminder(id, triggerAtMillis)
        }
    }

    private fun extractUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val matcher = WEB_URL.matcher(text)
        if (!matcher.find()) return null
        val raw = matcher.group()
        return if (raw.startsWith("http")) raw else "https://$raw"
    }

    private companion object {
        // android.util.Patterns is unavailable in plain unit tests; this is enough
        // for share-sheet text.
        val WEB_URL: Pattern =
            Pattern.compile("(https?://\\S+)|((www\\.)?[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}\\S*)")
    }
}
