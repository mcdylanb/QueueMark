package com.bookmarkapp.queuemark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bookmarkapp.queuemark.data.local.GuestSessionStore
import com.bookmarkapp.queuemark.data.remote.AuthRepository
import com.bookmarkapp.queuemark.ui.navigation.QueuemarkDestinations
import com.bookmarkapp.queuemark.ui.navigation.QueuemarkNavGraph
import com.bookmarkapp.queuemark.ui.share.NotificationPermissionEffect
import com.bookmarkapp.queuemark.ui.theme.QueuemarkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var guestStore: GuestSessionStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // A local guest session counts: entering the app must not require the
        // network-backed Firebase account (created lazily for sync only).
        val isSignedIn = authRepository.currentUserId != null || guestStore.isGuest

        // Persisted Firebase session (including anonymous) skips the auth screen.
        val startDestination = if (isSignedIn) {
            QueuemarkDestinations.DASHBOARD_ROUTE
        } else {
            QueuemarkDestinations.AUTH_ROUTE
        }

        // Set when the user taps a reading-reminder notification.
        val deepLinkBookmarkId =
            intent.getStringExtra(EXTRA_BOOKMARK_ID).takeIf { isSignedIn }

        setContent {
            QueuemarkTheme {
                NotificationPermissionEffect()
                QueuemarkNavGraph(
                    startDestination = startDestination,
                    deepLinkBookmarkId = deepLinkBookmarkId
                )
            }
        }
    }

    companion object {
        const val EXTRA_BOOKMARK_ID = "extra_bookmark_id"
    }
}
