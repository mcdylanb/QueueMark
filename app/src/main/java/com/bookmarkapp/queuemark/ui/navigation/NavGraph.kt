package com.bookmarkapp.queuemark.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.bookmarkapp.queuemark.ui.dashboard.DashboardRoute
import com.bookmarkapp.queuemark.ui.detail.DetailRoute

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
    deepLinkBookmarkId: String? = null,
    navController: NavHostController = rememberNavController()
) {
    // Notification tap lands directly on the bookmark it reminded about.
    LaunchedEffect(deepLinkBookmarkId) {
        if (deepLinkBookmarkId != null) {
            navController.navigate(QueuemarkDestinations.detailRoute(deepLinkBookmarkId))
        }
    }

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
            DashboardRoute(
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
        ) {
            DetailRoute(onNavigateBack = { navController.popBackStack() })
        }
    }
}
