package com.photobooth.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.photobooth.ui.screens.capture.CaptureScreen
import com.photobooth.ui.screens.capture.CaptureViewModel
import com.photobooth.ui.screens.filters.FilterScreen
import com.photobooth.ui.screens.filters.FilterViewModel
import com.photobooth.ui.screens.review.ReviewScreen

object Routes {
    const val CAPTURE = "capture"
    const val REVIEW = "review"
    const val FILTER = "filter"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.CAPTURE
    ) {
        composable(Routes.CAPTURE) { backStackEntry ->
            val captureViewModel: CaptureViewModel = hiltViewModel(backStackEntry)

            CaptureScreen(
                onPhotoCaptured = {
                    navController.navigate(Routes.REVIEW)
                },
                viewModel = captureViewModel
            )
        }

        composable(Routes.REVIEW) {
            // Get the CaptureViewModel from the previous backstack entry to access the photo
            val parentEntry = navController.getBackStackEntry(Routes.CAPTURE)
            val captureViewModel: CaptureViewModel = hiltViewModel(parentEntry)
            val uiState by captureViewModel.uiState.collectAsState()

            ReviewScreen(
                photo = uiState.capturedPhoto,
                onRetake = {
                    captureViewModel.resetCapture()
                    navController.popBackStack()
                },
                onAccept = {
                    navController.navigate(Routes.FILTER)
                }
            )
        }

        composable(Routes.FILTER) {
            val captureEntry = navController.getBackStackEntry(Routes.CAPTURE)
            val captureViewModel: CaptureViewModel = hiltViewModel(captureEntry)
            val uiState by captureViewModel.uiState.collectAsState()

            FilterScreen(
                photo = uiState.capturedPhoto,
                onBack = {
                    navController.popBackStack()
                },
                onDone = {
                    // For now, reset and go back to capture
                    // Later phases will navigate to share/print
                    captureViewModel.resetCapture()
                    navController.popBackStack(Routes.CAPTURE, inclusive = false)
                }
            )
        }
    }
}
