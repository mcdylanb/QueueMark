package com.bookmarkapp.queuemark.ui.auth

import com.bookmarkapp.queuemark.testutil.FakeAuthRepository
import com.bookmarkapp.queuemark.testutil.FakeBookmarkRepository
import com.bookmarkapp.queuemark.testutil.FakeGuestSessionStore
import com.bookmarkapp.queuemark.testutil.FakeSyncScheduler
import com.bookmarkapp.queuemark.testutil.MainDispatcherRule
import com.bookmarkapp.queuemark.testutil.collectEagerly
import com.bookmarkapp.queuemark.testutil.testBookmark
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    private val bookmarkRepository = FakeBookmarkRepository()
    private val syncScheduler = FakeSyncScheduler()

    private fun createViewModel() = AuthViewModel(
        authRepository = authRepository,
        guestStore = guestStore,
        bookmarkRepository = bookmarkRepository,
        syncScheduler = syncScheduler
    )

    private fun AuthViewModel.enterCredentials() {
        onAction(AuthUiAction.OnEmailChange("a@b.com"))
        onAction(AuthUiAction.OnPasswordChange("secret1"))
    }

    @Test
    fun `continue as guest enters the app even with no network`() = runTest {
        authRepository.failAnonymous = true // airplane mode
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.onAction(AuthUiAction.OnContinueOffline)

        assertTrue(viewModel.uiState.value.isAuthComplete)
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
    fun `plain email sign-in completes without prompts`() = runTest {
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.enterCredentials()
        viewModel.onAction(AuthUiAction.OnSignInClick)

        assertTrue(viewModel.uiState.value.isAuthComplete)
        assertFalse(viewModel.uiState.value.showSignInPrompt)
    }

    @Test
    fun `failed email sign-in surfaces the error`() = runTest {
        authRepository.failEmailSignIn = true
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.enterCredentials()
        viewModel.onAction(AuthUiAction.OnSignInClick)

        assertFalse(viewModel.uiState.value.isAuthComplete)
        assertTrue(viewModel.uiState.value.error != null)
    }

    @Test
    fun `anonymous user gets the erase warning before sign-in`() = runTest {
        authRepository.signInAnonymously()
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)
        bookmarkRepository.seed(testBookmark("guest-row"))

        viewModel.enterCredentials()
        viewModel.onAction(AuthUiAction.OnSignInClick)

        assertTrue(viewModel.uiState.value.showSignInPrompt)
        assertFalse(viewModel.uiState.value.isAuthComplete)
        assertFalse(bookmarkRepository.rows.value.isEmpty()) // nothing wiped yet

        viewModel.onAction(AuthUiAction.OnConfirmSignIn)

        assertTrue(bookmarkRepository.rows.value.isEmpty()) // erased on confirm
        assertTrue(viewModel.uiState.value.isAuthComplete)
    }

    @Test
    fun `anonymous sign-up prompt links account and requests sync`() = runTest {
        authRepository.signInAnonymously()
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)

        viewModel.enterCredentials()
        viewModel.onAction(AuthUiAction.OnSignUpClick)
        assertTrue(viewModel.uiState.value.showSignUpPrompt)

        viewModel.onAction(AuthUiAction.OnConfirmSignUp)

        assertEquals(1, authRepository.linkCalls)
        assertEquals(1, syncScheduler.syncRequests)
        assertTrue(viewModel.uiState.value.isAuthComplete)
    }

    @Test
    fun `dismissing a prompt keeps the session and data`() = runTest {
        authRepository.signInAnonymously()
        val viewModel = createViewModel()
        collectEagerly(viewModel.uiState)
        bookmarkRepository.seed(testBookmark("guest-row"))

        viewModel.enterCredentials()
        viewModel.onAction(AuthUiAction.OnSignInClick)
        viewModel.onAction(AuthUiAction.OnDismissPrompt)

        assertFalse(viewModel.uiState.value.showSignInPrompt)
        assertFalse(viewModel.uiState.value.isAuthComplete)
        assertFalse(bookmarkRepository.rows.value.isEmpty())
    }
}
