package com.bookmarkapp.queuemark.ui.auth

import com.bookmarkapp.queuemark.testutil.FakeAuthRepository
import com.bookmarkapp.queuemark.testutil.FakeGuestSessionStore
import com.bookmarkapp.queuemark.testutil.MainDispatcherRule
import com.bookmarkapp.queuemark.testutil.collectEagerly
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    private val guestStore = FakeGuestSessionStore()

    private fun createViewModel() = AuthViewModel(
        authRepository = authRepository,
        guestStore = guestStore
    )

    @Test
    fun `continue as guest enters the app even with no network`() = runTest {
        authRepository.failAnonymous = true // airplane mode
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.onAction(AuthUiAction.OnContinueOffline)

        assertTrue(viewModel.uiState.value.isAuthenticated)
        assertTrue(guestStore.isGuest)
        assertNull(viewModel.uiState.value.error) // failure is silent by design
    }

    @Test
    fun `continue as guest creates the account opportunistically when online`() = runTest {
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.onAction(AuthUiAction.OnContinueOffline)

        assertTrue(guestStore.isGuest)
        assertTrue(authRepository.user.value?.isAnonymous == true)
    }

    @Test
    fun `email sign-in does not set the guest flag`() = runTest {
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.onAction(AuthUiAction.OnEmailChange("a@b.com"))
        viewModel.onAction(AuthUiAction.OnPasswordChange("secret1"))
        viewModel.onAction(AuthUiAction.OnSignIn)

        assertTrue(viewModel.uiState.value.isAuthenticated)
        assertFalse(guestStore.isGuest)
    }

    @Test
    fun `failed email sign-in surfaces the error`() = runTest {
        authRepository.failEmailSignIn = true
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.onAction(AuthUiAction.OnEmailChange("a@b.com"))
        viewModel.onAction(AuthUiAction.OnPasswordChange("secret1"))
        viewModel.onAction(AuthUiAction.OnSignIn)

        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertTrue(viewModel.uiState.value.error != null)
    }
}
