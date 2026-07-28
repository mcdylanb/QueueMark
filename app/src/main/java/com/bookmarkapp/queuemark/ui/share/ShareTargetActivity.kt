package com.bookmarkapp.queuemark.ui.share

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookmarkapp.queuemark.ui.theme.QueuemarkTheme
import dagger.hilt.android.AndroidEntryPoint

// Receives ACTION_SEND text/plain from any browser's share sheet. The activity
// is translucent: only the reminder bottom sheet is visible over the caller.
@AndroidEntryPoint
class ShareTargetActivity : ComponentActivity() {

    private val viewModel: ShareViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.onSharedText(intent.getStringExtra(Intent.EXTRA_TEXT))

        setContent {
            QueuemarkTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                NotificationPermissionEffect()

                LaunchedEffect(state.isFinished) {
                    if (state.isFinished) finish()
                }

                ModalBottomSheet(onDismissRequest = { viewModel.onAction(ShareUiAction.OnDone) }) {
                    ScheduleReminderContent(state = state, onAction = viewModel::onAction)
                }
            }
        }
    }
}

// Reminders need POST_NOTIFICATIONS on API 33+; ask while the user is already
// interacting with the reminder sheet.
@Composable
fun NotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
