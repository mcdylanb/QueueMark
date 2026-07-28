package com.bookmarkapp.queuemark.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bookmarkapp.queuemark.ui.auth.AuthScreen
import com.bookmarkapp.queuemark.ui.auth.AuthViewModel
import com.bookmarkapp.queuemark.ui.dashboard.DashboardScreen

object QueuemarkDestinations {
    const val AUTH_ROUTE = "auth"
    const val DASHBOARD_ROUTE = "dashboard"
    const val DETAIL_ARG = "bookmarkId"
    const val DETAIL_ROUTE = "detail/{$DETAIL_ARG}"

    fun detailRoute(bookmarkId: String) = "detail/$bookmarkId"
}

@Composable
fun QueuemarkNavGraph(
    startDestination: String,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(QueuemarkDestinations.AUTH_ROUTE) {
            val viewModel: AuthViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(state.isAuthenticated) {
                if (state.isAuthenticated) {
                    navController.navigate(QueuemarkDestinations.DASHBOARD_ROUTE) {
                        popUpTo(QueuemarkDestinations.AUTH_ROUTE) { inclusive = true }
                    }
                }
            }

            AuthScreen(state = state, onAction = viewModel::onAction)
        }

        composable(QueuemarkDestinations.DASHBOARD_ROUTE) {
            DashboardScreen(
                onBookmarkClick = { id ->
                    navController.navigate(QueuemarkDestinations.detailRoute(id))
                }
            )
        }

        composable(
            route = QueuemarkDestinations.DETAIL_ROUTE,
            arguments = listOf(
                navArgument(QueuemarkDestinations.DETAIL_ARG) { type = NavType.StringType }
            )
        ) { entry ->
            DetailPlaceholder(
                bookmarkId = entry.arguments?.getString(QueuemarkDestinations.DETAIL_ARG).orEmpty()
            )
        }
    }
}

// Replaced by the real DetailScreen in Phase 4.
@Composable
private fun DetailPlaceholder(bookmarkId: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Detail for $bookmarkId arrives in Phase 4.")
    }
}
