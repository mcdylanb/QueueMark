package com.bookmarkapp.queuemark.ui.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookmarkapp.queuemark.ui.theme.QueuemarkTheme

// Placeholder so navigation is demonstrable; the real dashboard lands in Phase 4.
@Composable
fun DashboardScreen(
    onBookmarkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Hello, Reader",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Your queue arrives in Phase 4.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Preview(showBackground = true, name = "Dashboard — light")
@Composable
private fun DashboardScreenPreviewLight() {
    QueuemarkTheme(darkTheme = false) {
        DashboardScreen(onBookmarkClick = {})
    }
}

@Preview(
    showBackground = true,
    name = "Dashboard — dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun DashboardScreenPreviewDark() {
    QueuemarkTheme(darkTheme = true) {
        DashboardScreen(onBookmarkClick = {})
    }
}
