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
import com.photobooth.ui.screens.review.ReviewScreen

object Routes {
    const val CAPTURE = "capture"
    const val REVIEW = "review"
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
                    // For Phase 1, just go back to capture for now
                    // Later phases will navigate to filters/share
                    captureViewModel.resetCapture()
                    navController.popBackStack()
                }
            )
        }
    }
}
