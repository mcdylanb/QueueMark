package com.bookmarkapp.queuemark.ui.detail

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
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

    // In web mode the first back steps through WebView history; the final back
    // (and reader mode's only back) runs the session-end check.
    BackHandler {
        val view = webView
        if (state.viewMode == DetailViewMode.WEB && view != null && view.canGoBack()) {
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
                viewMode = state.viewMode,
                hasReaderContent = state.hasReaderContent,
                onBack = { onAction(DetailUiAction.OnExitRequested) },
                onToggleViewMode = { onAction(DetailUiAction.OnToggleViewMode) },
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
        val bookmark = state.bookmark
        when {
            bookmark == null -> Unit

            state.showOfflineEmptyState -> OfflineEmptyState(
                hasReaderContent = state.hasReaderContent,
                onOpenReader = { onAction(DetailUiAction.OnToggleViewMode) },
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            )

            state.viewMode == DetailViewMode.READER -> ReaderContent(
                bookmark = bookmark,
                onContentShown = { onAction(DetailUiAction.OnContentShown) },
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            )

            else -> AndroidView(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = object : WebViewClient() {
                            // onPageFinished fires even after an error; a failed
                            // main frame must not start the reading clock.
                            private var mainFrameFailed = false

                            override fun onPageFinished(view: WebView, url: String?) {
                                if (!mainFrameFailed) onAction(DetailUiAction.OnContentShown)
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError
                            ) {
                                if (request.isForMainFrame) {
                                    mainFrameFailed = true
                                    onAction(DetailUiAction.OnWebLoadFailed)
                                }
                            }
                        }
                        settings.javaScriptEnabled = true
                        if (ctx.isOffline()) {
                            settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                        }
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

private fun Context.isOffline(): Boolean {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return manager.activeNetwork == null
}

@Composable
private fun ReaderContent(
    bookmark: Bookmark,
    onContentShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) { onContentShown() }

    val progress = if (scrollState.maxValue > 0) {
        scrollState.value.toFloat() / scrollState.maxValue
    } else {
        0f
    }
    val domain = remember(bookmark.url) {
        bookmark.url.toUri().host?.removePrefix("www.") ?: bookmark.url
    }

    Column(modifier = modifier) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$domain • ${bookmark.estimatedReadTime} min read",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(20.dp))
            bookmark.content.orEmpty().split("\n\n").forEach { paragraph ->
                Text(
                    text = paragraph,
                    fontFamily = FontFamily.Serif,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun OfflineEmptyState(
    hasReaderContent: Boolean,
    onOpenReader: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "You're offline",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (hasReaderContent) {
                    "This page needs a connection, but an offline copy is available."
                } else {
                    "This article wasn't saved for offline reading. " +
                        "Reconnect to load the page."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
            if (hasReaderContent) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onOpenReader) {
                    Text("Read offline copy")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    bookmark: Bookmark?,
    viewMode: DetailViewMode,
    hasReaderContent: Boolean,
    onBack: () -> Unit,
    onToggleViewMode: () -> Unit,
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
            if (hasReaderContent) {
                IconButton(onClick = onToggleViewMode) {
                    Icon(
                        imageVector = if (viewMode == DetailViewMode.READER) {
                            Icons.Filled.Public
                        } else {
                            Icons.AutoMirrored.Filled.Article
                        },
                        contentDescription = if (viewMode == DetailViewMode.READER) {
                            "Open web view"
                        } else {
                            "Open reader view"
                        }
                    )
                }
            }
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
    content = "Attention is the scarcest resource of the modern knowledge worker, " +
        "and every interface we build either protects it or spends it.\n\n" +
        "This piece explores how deliberate constraints — fewer notifications, " +
        "single-purpose screens, honest progress indicators — return focus to the reader.",
    estimatedReadTime = 7,
    createdAt = 0L,
    reminderTime = null,
    isCompleted = false,
    completedAt = null,
    isSynced = true
)

// Whole-screen web-mode previews are omitted because WebView cannot render in
// preview; Reader, offline state, and the top bar are the previewable parts.
@Preview(showBackground = true, name = "Reader — light")
@Composable
private fun ReaderContentPreviewLight() {
    QueuemarkTheme(darkTheme = false) {
        ReaderContent(bookmark = previewBookmark, onContentShown = {})
    }
}

@Preview(
    showBackground = true,
    name = "Reader — dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ReaderContentPreviewDark() {
    QueuemarkTheme(darkTheme = true) {
        ReaderContent(bookmark = previewBookmark, onContentShown = {})
    }
}

@Preview(showBackground = true, name = "Offline state — light, no copy")
@Composable
private fun OfflineEmptyStatePreviewLight() {
    QueuemarkTheme(darkTheme = false) {
        OfflineEmptyState(
            hasReaderContent = false,
            onOpenReader = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(
    showBackground = true,
    name = "Offline state — dark, offline copy available",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun OfflineEmptyStatePreviewDark() {
    QueuemarkTheme(darkTheme = true) {
        OfflineEmptyState(
            hasReaderContent = true,
            onOpenReader = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true, name = "Top bar — reader mode")
@Composable
private fun DetailTopBarPreview() {
    QueuemarkTheme(darkTheme = false) {
        DetailTopBar(
            bookmark = previewBookmark,
            viewMode = DetailViewMode.READER,
            hasReaderContent = true,
            onBack = {},
            onToggleViewMode = {},
            onToggleComplete = {},
            onShare = {}
        )
    }
}
