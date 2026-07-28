package com.bookmarkapp.queuemark.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookmarkapp.queuemark.data.remote.AuthRepository
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
    val isAuthenticated: Boolean = false
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.length >= 6 && !isLoading
}

sealed interface AuthUiAction {
    data class OnEmailChange(val value: String) : AuthUiAction
    data class OnPasswordChange(val value: String) : AuthUiAction
    data object OnSignIn : AuthUiAction
    data object OnSignUp : AuthUiAction
    data object OnContinueOffline : AuthUiAction
    data object OnErrorDismissed : AuthUiAction
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authState.collect { user ->
                _uiState.update { it.copy(isAuthenticated = user != null) }
            }
        }
    }

    fun onAction(action: AuthUiAction) {
        when (action) {
            is AuthUiAction.OnEmailChange ->
                _uiState.update { it.copy(email = action.value, error = null) }

            is AuthUiAction.OnPasswordChange ->
                _uiState.update { it.copy(password = action.value, error = null) }

            AuthUiAction.OnSignIn -> authenticate {
                authRepository.signInWithEmail(uiState.value.email.trim(), uiState.value.password)
            }

            AuthUiAction.OnSignUp -> authenticate {
                authRepository.signUpWithEmail(uiState.value.email.trim(), uiState.value.password)
            }

            AuthUiAction.OnContinueOffline -> authenticate {
                authRepository.signInAnonymously()
            }

            AuthUiAction.OnErrorDismissed -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun authenticate(block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            block()
                .onSuccess { _uiState.update { it.copy(isLoading = false) } }
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
