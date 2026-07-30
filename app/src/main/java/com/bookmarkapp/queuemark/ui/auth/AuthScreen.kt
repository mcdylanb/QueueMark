package com.bookmarkapp.queuemark.ui.auth

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.bookmarkapp.queuemark.ui.theme.QueuemarkTheme

@Composable
fun AuthScreen(
    state: AuthUiState,
    onAction: (AuthUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val fieldShape = RoundedCornerShape(12.dp)

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Bookmark.",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Zero Hoarding. Just Reading.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = { onAction(AuthUiAction.OnEmailChange(it)) },
                label = { Text("Email") },
                singleLine = true,
                shape = fieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = { onAction(AuthUiAction.OnPasswordChange(it)) },
                label = { Text("Password") },
                singleLine = true,
                shape = fieldShape,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                supportingText = { Text("At least 6 characters") },
                modifier = Modifier.fillMaxWidth()
            )

            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { onAction(AuthUiAction.OnSignInClick) },
                enabled = state.canSubmit,
                shape = fieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Sign In")
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { onAction(AuthUiAction.OnSignUpClick) },
                enabled = state.canSubmit,
                shape = fieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Create Account")
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            // Google sign-in is a documented stretch goal; button stays disabled.
            OutlinedButton(
                onClick = {},
                enabled = false,
                shape = fieldShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Continue with Google (coming soon)")
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = {
                    if (state.isAnonymous) onAction(AuthUiAction.OnBackToDashboardClick)
                    else onAction(AuthUiAction.OnContinueOffline)
                },
                enabled = !state.isLoading
            ) {
                Text(
                    text = if (state.isAnonymous) "Return as guest" else "Continue as guest",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        if (state.showSignInPrompt) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { onAction(AuthUiAction.OnDismissPrompt) },
                title = { Text("Log in to existing account?") },
                text = { Text("Logging into an existing account will permanently erase the offline bookmarks you currently have saved. Do you want to proceed?") },
                confirmButton = {
                    Button(onClick = { onAction(AuthUiAction.OnConfirmSignIn) }) { Text("Log In & Erase") }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(AuthUiAction.OnDismissPrompt) }) { Text("Cancel") }
                }
            )
        }

        if (state.showSignUpPrompt) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { onAction(AuthUiAction.OnDismissPrompt) },
                title = { Text("Save offline data?") },
                text = { Text("Your currently saved offline bookmarks will be permanently linked to this new account.") },
                confirmButton = {
                    Button(onClick = { onAction(AuthUiAction.OnConfirmSignUp) }) { Text("Create Account") }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(AuthUiAction.OnDismissPrompt) }) { Text("Cancel") }
                }
            )
        }
    }
}

@Preview(showBackground = true, name = "Auth — light")
@Composable
private fun AuthScreenPreviewLight() {
    QueuemarkTheme(darkTheme = false) {
        AuthScreen(state = AuthUiState(), onAction = {})
    }
}

@Preview(
    showBackground = true,
    name = "Auth — dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AuthScreenPreviewDark() {
    QueuemarkTheme(darkTheme = true) {
        AuthScreen(
            state = AuthUiState(email = "reader@example.com", error = "Wrong password"),
            onAction = {}
        )
    }
}
