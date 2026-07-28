package com.bookmarkapp.queuemark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bookmarkapp.queuemark.data.remote.AuthRepository
import com.bookmarkapp.queuemark.ui.navigation.QueuemarkDestinations
import com.bookmarkapp.queuemark.ui.navigation.QueuemarkNavGraph
import com.bookmarkapp.queuemark.ui.theme.QueuemarkTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Persisted Firebase session (including anonymous) skips the auth screen.
        val startDestination = if (authRepository.currentUserId != null) {
            QueuemarkDestinations.DASHBOARD_ROUTE
        } else {
            QueuemarkDestinations.AUTH_ROUTE
        }

        setContent {
            QueuemarkTheme {
                QueuemarkNavGraph(startDestination = startDestination)
            }
        }
    }
}
