package com.bookmarkapp.queuemark.ui.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookmarkapp.queuemark.domain.model.Bookmark
import com.bookmarkapp.queuemark.ui.dashboard.components.AddBookmarkBottomSheet
import com.bookmarkapp.queuemark.ui.dashboard.components.BookmarkCard
import com.bookmarkapp.queuemark.ui.dashboard.components.QuickWinCard
import com.bookmarkapp.queuemark.ui.theme.QueuemarkTheme

@Composable
fun DashboardRoute(
    onBookmarkClick: (String) -> Unit,
    onNavigateToAuth: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        state = state,
        onAction = { action ->
            viewModel.onAction(action)

            // trigger the navigation when the logout action fires
            if (action is DashboardUiAction.OnLogoutClick) {
                onNavigateToAuth()
            }
        },
        onBookmarkClick = onBookmarkClick
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onAction: (DashboardUiAction) -> Unit,
    onBookmarkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            onAction(DashboardUiAction.OnMessageShown)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction(DashboardUiAction.OnFabClick) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add bookmark")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                DashboardHeader(
                    userLabel = state.userLabel,
                    hasPendingSync = state.hasPendingSync,
                    isAnonymous = state.isAnonymous,
                    onLogoutClick = { onAction(DashboardUiAction.OnLogoutClick) },
                    onLinkAccountClick = { onAction(DashboardUiAction.OnLinkAccountClick) }
                )
            }

            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { onAction(DashboardUiAction.OnSearchQueryChange(it)) },
                    placeholder = { Text("Search your queue") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onAction(DashboardUiAction.OnSearchQueryChange("")) }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.isSearching) {
                if (state.searchResults.isEmpty()) {
                    item { EmptyHint("No bookmarks match \"${state.searchQuery}\"") }
                } else {
                    items(state.searchResults, key = { it.id }) { bookmark ->
                        BookmarkCard(
                            bookmark = bookmark,
                            onClick = { onBookmarkClick(bookmark.id) }
                        )
                    }
                }
            } else {
                if (state.quickWins.isNotEmpty()) {
                    item { QuickWinsSection(state.quickWins, onBookmarkClick) }
                }

                item {
                    TabRow(selectedTabIndex = state.selectedTab.ordinal) {
                        DashboardTab.entries.forEach { tab ->
                            Tab(
                                selected = state.selectedTab == tab,
                                onClick = { onAction(DashboardUiAction.OnTabSelected(tab)) },
                                text = {
                                    Text(
                                        if (tab == DashboardTab.UNREAD) {
                                            "Unread (${state.unread.size})"
                                        } else {
                                            "Completed (${state.completed.size})"
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                val list = if (state.selectedTab == DashboardTab.UNREAD) {
                    state.unread
                } else {
                    state.completed
                }
                if (list.isEmpty()) {
                    item {
                        EmptyHint(
                            if (state.selectedTab == DashboardTab.UNREAD) {
                                "Your queue is empty — share a link here or tap +"
                            } else {
                                "Nothing completed yet. You've got this."
                            }
                        )
                    }
                } else {
                    items(list, key = { it.id }) { bookmark ->
                        SwipeableBookmarkCard(
                            bookmark = bookmark,
                            onClick = { onBookmarkClick(bookmark.id) },
                            onDelete = { onAction(DashboardUiAction.OnDeleteBookmark(bookmark.id)) },
                            onToggleComplete = {
                                onAction(DashboardUiAction.OnToggleComplete(bookmark.id))
                            }
                        )
                    }
                }
            }
        }
    }

    if (state.isAddSheetVisible) {
        AddBookmarkBottomSheet(
            isSaving = state.isSavingBookmark,
            onSave = { url, title -> onAction(DashboardUiAction.OnAddBookmark(url, title)) },
            onDismiss = { onAction(DashboardUiAction.OnDismissSheet) }
        )
    }

    if (state.isLinkAccountDialogVisible) {
        LinkAccountDialog(
            isLinking = state.isLinking,
            onDismiss = { onAction(DashboardUiAction.OnDismissLinkDialog) },
            onSubmit = { email, password ->
                onAction(DashboardUiAction.OnSubmitLinkAccount(email, password))
            }
        )
    }
}

@Composable
private fun LinkAccountDialog(
    isLinking: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLinking) onDismiss() },
        title = { Text("Save your bookmarks") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Create an account to save your offline queue permanently.")
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onSubmit(email.trim(), password) },
                enabled = email.isNotBlank() && password.length >= 6 && !isLinking
            ) {
                if (isLinking) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Account")
                }
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss,
                enabled = !isLinking
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DashboardHeader(
    userLabel: String,
    hasPendingSync: Boolean,
    isAnonymous: Boolean,
    onLogoutClick: () -> Unit,
    onLinkAccountClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hello, $userLabel",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (hasPendingSync) "Changes waiting to sync" else "All caught up",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Icon(
            imageVector = if (hasPendingSync) Icons.Filled.CloudSync else Icons.Filled.CloudDone,
            contentDescription = if (hasPendingSync) "Sync pending" else "Synced",
            tint = if (hasPendingSync) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.secondary
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        if (isAnonymous) {
            IconButton(onClick = onLinkAccountClick) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = "Save Account",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            IconButton(onClick = onLogoutClick) {
                Icon(
                    imageVector = Icons.Filled.Logout,
                    contentDescription = "Log Out",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun QuickWinsSection(quickWins: List<Bookmark>, onBookmarkClick: (String) -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Quick Wins",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "< 5 min",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(quickWins, key = { it.id }) { bookmark ->
                QuickWinCard(
                    bookmark = bookmark,
                    onClick = { onBookmarkClick(bookmark.id) },
                    modifier = Modifier
                        .width(250.dp)
                        .height(125.dp)
                )
            }
        }
    }
}

// Swipe start->end toggles complete (box snaps back); end->start deletes.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableBookmarkCard(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }

                SwipeToDismissBoxValue.StartToEnd -> {
                    onToggleComplete()
                    false
                }

                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val (color, icon, alignment) = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (bookmark.isCompleted) {
                        // Completed -> Unread
                        Triple(
                            MaterialTheme.colorScheme.secondary,
                            Icons.AutoMirrored.Filled.Undo,
                            Alignment.CenterStart
                        )
                    } else {
                        // Unread -> Completed
                        Triple(
                            MaterialTheme.colorScheme.primary,
                            Icons.Filled.CheckCircle,
                            Alignment.CenterStart
                        )
                    }
                }

                else -> Triple(
                    MaterialTheme.colorScheme.error,
                    Icons.Filled.Delete,
                    Alignment.CenterEnd
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.surface)
            }
        }
    ) {
        BookmarkCard(bookmark = bookmark, onClick = onClick)
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

private val previewBookmarks = listOf(
    Bookmark(
        id = "1",
        url = "https://medium.com/focus",
        title = "Designing for Focus in the Age of Distraction",
        description = null,
        estimatedReadTime = 3,
        createdAt = 1L,
        reminderTime = null,
        isCompleted = false,
        completedAt = null,
        isSynced = true
    ),
    Bookmark(
        id = "2",
        url = "https://nngroup.com/articles",
        title = "The 5-Minute Morning Ritual for Productive PMs",
        description = null,
        estimatedReadTime = 12,
        createdAt = 2L,
        reminderTime = null,
        isCompleted = false,
        completedAt = null,
        isSynced = false
    )
)

@Preview(showBackground = true, name = "Dashboard — light")
@Composable
private fun DashboardScreenPreviewLight() {
    QueuemarkTheme(darkTheme = false) {
        DashboardScreen(
            state = DashboardUiState(
                quickWins = previewBookmarks.take(1),
                unread = previewBookmarks,
                hasPendingSync = true
            ),
            onAction = {},
            onBookmarkClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Dashboard — dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardScreenPreviewDark() {
    QueuemarkTheme(darkTheme = true) {
        DashboardScreen(
            state = DashboardUiState(unread = previewBookmarks),
            onAction = {},
            onBookmarkClick = {}
        )
    }
}
