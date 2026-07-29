package com.bookmarkapp.queuemark.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookmarkapp.queuemark.data.remote.AuthRepository
import com.bookmarkapp.queuemark.data.remote.UrlMetadataService
import com.bookmarkapp.queuemark.data.repository.BookmarkRepository
import com.bookmarkapp.queuemark.domain.ReadTimeCalculator
import com.bookmarkapp.queuemark.domain.TimeProvider
import com.bookmarkapp.queuemark.domain.model.Bookmark
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DashboardTab { UNREAD, COMPLETED }

data class DashboardUiState(
    val userLabel: String = "Reader",
    val quickWins: List<Bookmark> = emptyList(),
    val unread: List<Bookmark> = emptyList(),
    val completed: List<Bookmark> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Bookmark> = emptyList(),
    val selectedTab: DashboardTab = DashboardTab.UNREAD,
    val isAddSheetVisible: Boolean = false,
    val isSavingBookmark: Boolean = false,
    val hasPendingSync: Boolean = false,
    val userMessage: String? = null,
    val isAnonymous: Boolean = false,
    val isLinkAccountDialogVisible: Boolean = false,
    val isLinking: Boolean = false
) {
    val isSearching: Boolean get() = searchQuery.isNotBlank()
}

sealed interface DashboardUiAction {
    data class OnTabSelected(val tab: DashboardTab) : DashboardUiAction
    data class OnSearchQueryChange(val query: String) : DashboardUiAction
    data class OnDeleteBookmark(val id: String) : DashboardUiAction
    data class OnToggleComplete(val id: String) : DashboardUiAction
    data object OnFabClick : DashboardUiAction
    data object OnDismissSheet : DashboardUiAction
    data class OnAddBookmark(val url: String, val title: String?) : DashboardUiAction
    data object OnMessageShown : DashboardUiAction
    data object OnLogoutClick : DashboardUiAction
    data object OnLinkAccountClick : DashboardUiAction
    data object OnDismissLinkDialog : DashboardUiAction
    data class OnSubmitLinkAccount(val email: String, val password: String) : DashboardUiAction
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: BookmarkRepository,
    private val metadataService: UrlMetadataService,
    private val timeProvider: TimeProvider,
    private val authRepository: AuthRepository
) : ViewModel() {

    private data class Controls(
        val userLabel: String = "Reader",
        val searchQuery: String = "",
        val selectedTab: DashboardTab = DashboardTab.UNREAD,
        val isAddSheetVisible: Boolean = false,
        val isSavingBookmark: Boolean = false,
        val userMessage: String? = null,
        val isAnonymous: Boolean = false,
        val isLinkAccountDialogVisible: Boolean = false,
        val isLinking: Boolean = false
    )

    private val controls = MutableStateFlow(Controls())

    private val searchResults = controls
        .map { it.searchQuery }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList()) else repository.search(query)
        }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeQuickWins(),
        repository.observeActive(),
        repository.observeCompleted(),
        searchResults,
        controls
    ) { quickWins, unread, completed, results, c ->
        DashboardUiState(
            userLabel = c.userLabel,
            quickWins = quickWins,
            unread = unread,
            completed = completed,
            searchQuery = c.searchQuery,
            searchResults = results,
            selectedTab = c.selectedTab,
            isAddSheetVisible = c.isAddSheetVisible,
            isSavingBookmark = c.isSavingBookmark,
            hasPendingSync = (unread + completed).any { !it.isSynced },
            userMessage = c.userMessage,
            isAnonymous = c.isAnonymous,
            isLinkAccountDialogVisible = c.isLinkAccountDialogVisible,
            isLinking = c.isLinking
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    init {
        viewModelScope.launch {
            authRepository.authState.collect { user ->
                controls.update {
                    it.copy(
                        userLabel = user?.email?.substringBefore('@')?.takeIf { p -> p.isNotBlank() }
                            ?: if (user?.isAnonymous == true) "Guest" else "Reader",
                        isAnonymous = user?.isAnonymous ?: false // Track anonymous state
                    )
                }
            }
        }
    }

    fun onAction(action: DashboardUiAction) {
        when (action) {
            is DashboardUiAction.OnTabSelected ->
                controls.update { it.copy(selectedTab = action.tab) }

            is DashboardUiAction.OnSearchQueryChange ->
                controls.update { it.copy(searchQuery = action.query) }

            DashboardUiAction.OnFabClick ->
                controls.update { it.copy(isAddSheetVisible = true) }

            DashboardUiAction.OnDismissSheet ->
                controls.update { it.copy(isAddSheetVisible = false) }

            is DashboardUiAction.OnAddBookmark -> addBookmark(action.url, action.title)

            is DashboardUiAction.OnDeleteBookmark -> viewModelScope.launch {
                repository.delete(action.id)
                    .onFailure { showMessage("Couldn't delete bookmark") }
            }

            is DashboardUiAction.OnToggleComplete -> viewModelScope.launch {
                val current = uiState.value.let { s ->
                    (s.unread + s.completed + s.searchResults).firstOrNull { it.id == action.id }
                } ?: return@launch
                repository.setCompleted(action.id, !current.isCompleted)
                    .onFailure { showMessage("Couldn't update bookmark") }
            }

            is DashboardUiAction.OnLogoutClick -> {
                viewModelScope.launch {
                    repository.clearAll() // clear cache

                    authRepository.signOut() // sign out of firebase
                }
            }

            DashboardUiAction.OnLinkAccountClick ->
                controls.update { it.copy(isLinkAccountDialogVisible = true) }

            DashboardUiAction.OnDismissLinkDialog ->
                controls.update { it.copy(isLinkAccountDialogVisible = false) }

            is DashboardUiAction.OnSubmitLinkAccount -> viewModelScope.launch {
                controls.update { it.copy(isLinking = true) }
                authRepository.linkWithEmail(action.email, action.password)
                    .onSuccess {
                        controls.update {
                            it.copy(
                                isLinking = false,
                                isLinkAccountDialogVisible = false,
                                userMessage = "Account saved!",
                                isAnonymous = false,
                                userLabel = action.email.substringBefore('@')
                            )
                        }
                    }
                    .onFailure { error ->
                        controls.update {
                            it.copy(
                                isLinking = false,
                                userMessage = error.localizedMessage ?: "Failed to link account"
                            )
                        }
                    }
            }

            DashboardUiAction.OnMessageShown ->
                controls.update { it.copy(userMessage = null) }
        }
    }

    private fun addBookmark(rawUrl: String, manualTitle: String?) {
        val url = normalizeUrl(rawUrl) ?: run {
            showMessage("Enter a valid link")
            return
        }
        viewModelScope.launch {
            controls.update { it.copy(isSavingBookmark = true) }

            // Offline-first: a failed scrape must never lose the bookmark —
            // fall back to the URL as title and the minimum read time.
            val metadata = metadataService.fetch(url).getOrNull()
            val bookmark = Bookmark(
                id = UUID.randomUUID().toString(),
                url = url,
                title = manualTitle?.takeIf { it.isNotBlank() }
                    ?: metadata?.title
                    ?: url,
                description = metadata?.description,
                estimatedReadTime = ReadTimeCalculator.estimateMinutes(metadata?.wordCount ?: 0),
                createdAt = timeProvider.now(),
                reminderTime = null,
                isCompleted = false,
                completedAt = null,
                isSynced = false
            )

            repository.add(bookmark)
                .onSuccess {
                    controls.update {
                        it.copy(
                            isSavingBookmark = false,
                            isAddSheetVisible = false,
                            userMessage = "Saved to your queue"
                        )
                    }
                }
                .onFailure {
                    controls.update { it.copy(isSavingBookmark = false) }
                    showMessage("Couldn't save bookmark")
                }
        }
    }

    private fun showMessage(message: String) {
        controls.update { it.copy(userMessage = message) }
    }

    private fun normalizeUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains('.') && !trimmed.contains(' ') -> "https://$trimmed"
            else -> null
        }
    }
}
