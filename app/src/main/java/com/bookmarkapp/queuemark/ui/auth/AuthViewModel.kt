package com.bookmarkapp.queuemark.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookmarkapp.queuemark.data.local.GuestSessionStore
import com.bookmarkapp.queuemark.data.remote.AuthRepository
import com.bookmarkapp.queuemark.data.remote.SyncScheduler
import com.bookmarkapp.queuemark.data.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAnonymous: Boolean = false,
    val showSignUpPrompt: Boolean = false,
    val showSignInPrompt: Boolean = false,
    val isAuthComplete: Boolean = false
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.length >= 6 && !isLoading
}

sealed interface AuthUiAction {
    data class OnEmailChange(val value: String) : AuthUiAction
    data class OnPasswordChange(val value: String) : AuthUiAction
    data object OnSignInClick : AuthUiAction
    data object OnSignUpClick : AuthUiAction
    data object OnConfirmSignIn : AuthUiAction
    data object OnConfirmSignUp : AuthUiAction
    data object OnDismissPrompt : AuthUiAction
    data object OnBackToDashboardClick : AuthUiAction
    data object OnContinueOffline : AuthUiAction
    data object OnErrorDismissed : AuthUiAction
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val guestStore: GuestSessionStore,
    private val bookmarkRepository: BookmarkRepository, // wipes data on Sign In
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authState.collect { user ->
                _uiState.update { it.copy(isAnonymous = user?.isAnonymous == true) }
            }
        }
    }

    fun onAction(action: AuthUiAction) {
        when (action) {
            is AuthUiAction.OnEmailChange ->
                _uiState.update { it.copy(email = action.value, error = null) }

            is AuthUiAction.OnPasswordChange ->
                _uiState.update { it.copy(password = action.value, error = null) }

            AuthUiAction.OnSignInClick -> {
                if (uiState.value.isAnonymous) {
                    _uiState.update { it.copy(showSignInPrompt = true) }
                } else {
                    authenticate { authRepository.signInWithEmail(uiState.value.email.trim(), uiState.value.password) }
                }
            }

            AuthUiAction.OnSignUpClick -> {
                if (uiState.value.isAnonymous) {
                    _uiState.update { it.copy(showSignUpPrompt = true) }
                } else {
                    authenticate { authRepository.signUpWithEmail(uiState.value.email.trim(), uiState.value.password) }
                }
            }

            AuthUiAction.OnConfirmSignIn -> authenticate {
                _uiState.update { it.copy(showSignInPrompt = false) }
                val res = authRepository.signInWithEmail(uiState.value.email.trim(), uiState.value.password) // 1. Log in

                if (res.isSuccess) {
                    bookmarkRepository.clearAll() // 2. Erase Guest Data
                }

                res
            }

            // Guest entry must work with zero network: set the local flag and
            // navigate immediately. The anonymous Firebase account (needed only
            // for sync) is created opportunistically here and lazily on later
            // app starts — see QueuemarkApplication.
            AuthUiAction.OnContinueOffline -> {
                guestStore.isGuest = true
                _uiState.update { it.copy(isAuthComplete = true) }
                viewModelScope.launch {
                    authRepository.signInAnonymously() // best-effort; offline is fine
                }
            }

            AuthUiAction.OnConfirmSignUp -> authenticate {
                _uiState.update { it.copy(showSignUpPrompt = false) }
                val result = authRepository.linkWithEmail(uiState.value.email.trim(), uiState.value.password) // Save data to new account

                if (result.isSuccess) {
                    syncScheduler.requestSync() // sync data to firebase
                }

                result
            }

            AuthUiAction.OnDismissPrompt ->
                _uiState.update { it.copy(showSignInPrompt = false, showSignUpPrompt = false) }

            AuthUiAction.OnBackToDashboardClick ->
                _uiState.update { it.copy(isAuthComplete = true) }

            AuthUiAction.OnErrorDismissed -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun authenticate(block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            block()
                .onSuccess { _uiState.update { it.copy(isLoading = false, isAuthComplete = true) } }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.localizedMessage ?: "Authentication failed"
                        )
                    }
                }
        }
    }
}
