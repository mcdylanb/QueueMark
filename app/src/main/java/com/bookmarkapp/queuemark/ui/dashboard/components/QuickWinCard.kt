package com.bookmarkapp.queuemark.ui.dashboard.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bookmarkapp.queuemark.domain.model.Bookmark
import com.bookmarkapp.queuemark.ui.theme.QueuemarkTheme

@Composable
fun QuickWinCard(
    bookmark: Bookmark,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary),
        modifier = modifier.width(260.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Have ${bookmark.estimatedReadTime} minute" +
                        (if (bookmark.estimatedReadTime == 1) "" else "s") +
                        "? Finish this article.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${bookmark.estimatedReadTime} min read",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

private val previewBookmark = Bookmark(
    id = "1",
    url = "https://example.com/tokens",
    title = "Understanding M3 Design Tokens in 180 Seconds",
    description = null,
    estimatedReadTime = 3,
    createdAt = 0L,
    reminderTime = null,
    isCompleted = false,
    completedAt = null,
    isSynced = true
)

@Preview(showBackground = true, name = "QuickWin — light")
@Composable
private fun QuickWinCardPreviewLight() {
    QueuemarkTheme(darkTheme = false) {
        QuickWinCard(bookmark = previewBookmark, onClick = {})
    }
}

@Preview(showBackground = true, name = "QuickWin — dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuickWinCardPreviewDark() {
    QueuemarkTheme(darkTheme = true) {
        QuickWinCard(bookmark = previewBookmark, onClick = {})
    }
}
