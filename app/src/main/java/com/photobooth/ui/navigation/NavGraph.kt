package com.photobooth.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.photobooth.ui.screens.attract.AttractScreen
import com.photobooth.ui.screens.capture.CaptureScreen
import com.photobooth.ui.screens.capture.CaptureViewModel
import com.photobooth.ui.screens.collage.CollageScreen
import com.photobooth.ui.screens.collage.CollageViewModel
import com.photobooth.ui.screens.filters.FilterScreen
import com.photobooth.ui.screens.filters.FilterViewModel
import com.photobooth.ui.screens.gif.GifScreen
import com.photobooth.ui.screens.gif.GifViewModel
import com.photobooth.ui.screens.modeselect.ModeSelectScreen
import com.photobooth.ui.screens.review.ReviewScreen
import com.photobooth.ui.screens.share.ShareScreen

object Routes {
    const val ATTRACT = "attract"
    const val MODE_SELECT = "mode_select"
    const val CAPTURE = "capture"
    const val REVIEW = "review"
    const val FILTER = "filter"
    const val SHARE = "share"
    const val COLLAGE = "collage"
    const val GIF = "gif"
    const val GIF_CAPTURE = "gif_capture"
    const val COLLAGE_CAPTURE = "collage_capture"
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.ATTRACT
    ) {
        // Attract / idle screen
        composable(Routes.ATTRACT) {
            AttractScreen(
                onTap = { navController.navigate(Routes.MODE_SELECT) }
            )
        }

        // Mode selection
        composable(Routes.MODE_SELECT) {
            ModeSelectScreen(
                onSinglePhoto = { navController.navigate(Routes.CAPTURE) },
                onCollage = { navController.navigate(Routes.COLLAGE) },
                onGif = { navController.navigate(Routes.GIF) }
            )
        }

        // --- Single Photo Flow ---
        composable(Routes.CAPTURE) { backStackEntry ->
            val captureViewModel: CaptureViewModel = hiltViewModel(backStackEntry)
            CaptureScreen(
                onPhotoCaptured = { navController.navigate(Routes.REVIEW) },
                viewModel = captureViewModel
            )
        }

        composable(Routes.REVIEW) {
            val parentEntry = navController.getBackStackEntry(Routes.CAPTURE)
            val captureViewModel: CaptureViewModel = hiltViewModel(parentEntry)
            val uiState by captureViewModel.uiState.collectAsState()

            ReviewScreen(
                photo = uiState.capturedPhoto,
                onRetake = {
                    captureViewModel.resetCapture()
                    navController.popBackStack()
                },
                onAccept = { navController.navigate(Routes.FILTER) }
            )
        }

        composable(Routes.FILTER) {
            val captureEntry = navController.getBackStackEntry(Routes.CAPTURE)
            val captureViewModel: CaptureViewModel = hiltViewModel(captureEntry)
            val uiState by captureViewModel.uiState.collectAsState()
            val filterViewModel: FilterViewModel = hiltViewModel()

            FilterScreen(
                photo = uiState.capturedPhoto,
                onBack = { navController.popBackStack() },
                onDone = { navController.navigate(Routes.SHARE) },
                viewModel = filterViewModel
            )
        }

        composable(Routes.SHARE) {
            // Get processed photo from filter VM if available, else from capture
            val captureEntry = navController.getBackStackEntry(Routes.CAPTURE)
            val captureViewModel: CaptureViewModel = hiltViewModel(captureEntry)
            val uiState by captureViewModel.uiState.collectAsState()

            ShareScreen(
                photo = uiState.capturedPhoto,
                onDone = {
                    captureViewModel.resetCapture()
                    navController.popBackStack(Routes.ATTRACT, inclusive = false)
                }
            )
        }

        // --- Collage Flow ---
        composable(Routes.COLLAGE) { backStackEntry ->
            val collageViewModel: CollageViewModel = hiltViewModel(backStackEntry)
            val captureEntry = try {
                navController.getBackStackEntry(Routes.COLLAGE_CAPTURE)
            } catch (e: Exception) { null }

            val captureViewModel: CaptureViewModel? = captureEntry?.let { hiltViewModel(it) }
            val captureState = captureViewModel?.uiState?.collectAsState()

            // If returning from capture with a photo, add it
            val newPhoto = captureState?.value?.capturedPhoto
            if (newPhoto != null) {
                collageViewModel.addPhoto(newPhoto)
                captureViewModel.resetCapture()
            }

            CollageScreen(
                initialPhoto = null,
                onTakeMore = { navController.navigate(Routes.COLLAGE_CAPTURE) },
                onDone = { collageBitmap ->
                    // Navigate to share with the collage bitmap
                    navController.navigate(Routes.SHARE)
                },
                onCancel = {
                    collageViewModel.reset()
                    navController.popBackStack(Routes.MODE_SELECT, inclusive = false)
                },
                viewModel = collageViewModel
            )
        }

        composable(Routes.COLLAGE_CAPTURE) { backStackEntry ->
            val captureViewModel: CaptureViewModel = hiltViewModel(backStackEntry)
            CaptureScreen(
                onPhotoCaptured = {
                    navController.popBackStack()
                },
                viewModel = captureViewModel
            )
        }

        // --- GIF Flow ---
        composable(Routes.GIF) { backStackEntry ->
            val gifViewModel: GifViewModel = hiltViewModel(backStackEntry)
            val captureEntry = try {
                navController.getBackStackEntry(Routes.GIF_CAPTURE)
            } catch (e: Exception) { null }

            val captureViewModel: CaptureViewModel? = captureEntry?.let { hiltViewModel(it) }
            val captureState = captureViewModel?.uiState?.collectAsState()

            val newFrame = captureState?.value?.capturedPhoto
            if (newFrame != null) {
                gifViewModel.addFrame(newFrame)
                captureViewModel.resetCapture()
            }

            GifScreen(
                initialFrame = null,
                onTakeMore = { navController.navigate(Routes.GIF_CAPTURE) },
                onDone = { gifFile ->
                    navController.popBackStack(Routes.ATTRACT, inclusive = false)
                },
                onCancel = {
                    gifViewModel.reset()
                    navController.popBackStack(Routes.MODE_SELECT, inclusive = false)
                },
                viewModel = gifViewModel
            )
        }

        composable(Routes.GIF_CAPTURE) { backStackEntry ->
            val captureViewModel: CaptureViewModel = hiltViewModel(backStackEntry)
            CaptureScreen(
                onPhotoCaptured = {
                    navController.popBackStack()
                },
                viewModel = captureViewModel
            )
        }
    }
}
