package com.bookmarkapp.queuemark.ui.detail

import android.content.Intent
import android.content.res.Configuration
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookmarkapp.queuemark.domain.model.Bookmark
import com.bookmarkapp.queuemark.ui.theme.QueuemarkTheme

@Composable
fun DetailRoute(
    onNavigateBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.closeScreen) {
        if (state.closeScreen) onNavigateBack()
    }

    DetailScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun DetailScreen(
    state: DetailUiState,
    onAction: (DetailUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }

    // First back closes WebView history; the final back runs the session-end check.
    BackHandler {
        val view = webView
        if (view != null && view.canGoBack()) {
            view.goBack()
        } else {
            onAction(DetailUiAction.OnExitRequested)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DetailTopBar(
                bookmark = state.bookmark,
                onBack = { onAction(DetailUiAction.OnExitRequested) },
                onToggleComplete = { onAction(DetailUiAction.OnToggleComplete) },
                onShare = {
                    state.bookmark?.let { bookmark ->
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, bookmark.url)
                            putExtra(Intent.EXTRA_TITLE, bookmark.title)
                        }
                        context.startActivity(Intent.createChooser(send, "Share bookmark"))
                    }
                }
            )
        }
    ) { innerPadding ->
        state.bookmark?.let { bookmark ->
            AndroidView(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        webView = this
                    }
                },
                update = { view ->
                    if (view.url == null) view.loadUrl(bookmark.url)
                }
            )
        }
    }

    if (state.showCompletePrompt) {
        AlertDialog(
            onDismissRequest = { onAction(DetailUiAction.OnDismissPrompt) },
            title = { Text("Nice reading session!") },
            text = { Text("We noticed you read this. Mark as complete?") },
            confirmButton = {
                TextButton(onClick = { onAction(DetailUiAction.OnConfirmComplete) }) {
                    Text("Mark complete")
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(DetailUiAction.OnDismissPrompt) }) {
                    Text("Not yet")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    bookmark: Bookmark?,
    onBack: () -> Unit,
    onToggleComplete: () -> Unit,
    onShare: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = bookmark?.title.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onToggleComplete) {
                Icon(
                    imageVector = if (bookmark?.isCompleted == true) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Outlined.CheckCircle
                    },
                    contentDescription = if (bookmark?.isCompleted == true) {
                        "Mark as unread"
                    } else {
                        "Mark as complete"
                    },
                    tint = if (bookmark?.isCompleted == true) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = "Share")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

private val previewBookmark = Bookmark(
    id = "1",
    url = "https://example.com/article",
    title = "Designing for Focus in the Age of Distraction",
    description = null,
    estimatedReadTime = 7,
    createdAt = 0L,
    reminderTime = null,
    isCompleted = false,
    completedAt = null,
    isSynced = true
)

// Whole-screen previews are omitted because WebView cannot render in preview;
// the top bar is the previewable part.
@Preview(showBackground = true, name = "Detail top bar — light")
@Composable
private fun DetailTopBarPreviewLight() {
    QueuemarkTheme(darkTheme = false) {
        DetailTopBar(
            bookmark = previewBookmark,
            onBack = {},
            onToggleComplete = {},
            onShare = {}
        )
    }
}

@Preview(
    showBackground = true,
    name = "Detail top bar — dark, completed",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun DetailTopBarPreviewDark() {
    QueuemarkTheme(darkTheme = true) {
        DetailTopBar(
            bookmark = previewBookmark.copy(isCompleted = true),
            onBack = {},
            onToggleComplete = {},
            onShare = {}
        )
    }
}
