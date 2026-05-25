package com.snapcabin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.snapcabin.collage.CollageLayout
import com.snapcabin.collage.CollageRenderer
import com.snapcabin.settings.BoothSettings
import com.snapcabin.settings.SettingsManager
import com.snapcabin.ui.components.InactivityHandler
import com.snapcabin.ui.screens.admin.AdminScreen
import com.snapcabin.ui.screens.admin.AdminViewModel
import com.snapcabin.ui.screens.attract.AttractScreen
import com.snapcabin.ui.screens.capture.CaptureMode
import com.snapcabin.ui.screens.capture.CaptureScreen
import com.snapcabin.ui.screens.capture.CaptureViewModel
import com.snapcabin.ui.screens.gallery.GalleryScreen
import com.snapcabin.ui.screens.gallery.GalleryViewModel
import com.snapcabin.ui.screens.getready.GetReadyScreen
import com.snapcabin.ui.screens.modeselect.ModeSelectScreen
import com.snapcabin.ui.screens.privacy.PrivacyPolicyScreen
import com.snapcabin.ui.screens.review.ReviewScreen
import com.snapcabin.ui.screens.share.ShareScreen

object Routes {
    const val ATTRACT = "attract"
    const val MODE_SELECT = "mode_select"
    const val GET_READY = "get_ready/{mode}"
    const val CAPTURE = "capture/{mode}"
    const val REVIEW = "review"
    const val SHARE = "share"
    const val ADMIN = "admin"
    const val PRIVACY = "privacy"
    const val GALLERY = "gallery"

    fun getReady(mode: CaptureMode) = "get_ready/${mode.routeArg}"
    fun capture(mode: CaptureMode) = "capture/${mode.routeArg}"
}

private fun getScreenTimeout(route: String?): Long = when {
    route == Routes.MODE_SELECT -> 30_000L
    route?.startsWith("get_ready") == true -> 60_000L
    route?.startsWith("capture") == true -> 90_000L
    route == Routes.REVIEW -> 30_000L
    route == Routes.SHARE -> 60_000L
    else -> 0L
}

private fun getWarningDuration(route: String?): Long = when (route) {
    Routes.REVIEW -> 10_000L
    else -> 15_000L
}

private fun getStartRoute(settings: BoothSettings): String {
    val enabledModes = mutableListOf<CaptureMode>()
    if (settings.enableSinglePhotoMode) enabledModes.add(CaptureMode.Single)
    if (settings.enableCollageMode) enabledModes.add(CaptureMode.Collage)
    if (settings.enableGifMode) enabledModes.add(CaptureMode.Gif)
    if (enabledModes.isEmpty()) return Routes.MODE_SELECT
    return if (enabledModes.size == 1) Routes.getReady(enabledModes.first()) else Routes.MODE_SELECT
}

@Composable
fun NavGraph(settingsManager: SettingsManager) {
    val navController = rememberNavController()
    val settings by settingsManager.settings.collectAsState(
        initial = BoothSettings()
    )

    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    val timeoutMs = remember(currentRoute) { getScreenTimeout(currentRoute) }
    val warningMs = remember(currentRoute) { getWarningDuration(currentRoute) }
    val timeoutEnabled = timeoutMs > 0

    // Single source of truth for "go back to the Attract screen." Used by
    // both the inactivity timeout and Share's session-end event.
    //
    // We use the canonical navigate(start) { popUpTo(start) { inclusive = false }
    // launchSingleTop = true } pattern from the Compose Navigation docs.
    // popUpTo with inclusive=false clears every entry above ATTRACT while
    // keeping ATTRACT itself, and launchSingleTop reuses the existing
    // ATTRACT entry instead of pushing a duplicate. The back stack never
    // empties, so neither the activity nor the system task manager treats
    // this as "user wants to leave the app."
    //
    // Plain popBackStack(Routes.ATTRACT, inclusive = false) looks equivalent
    // but in practice triggered an activity finish in Navigation Compose
    // 2.8.5 when ATTRACT was the start destination of the NavHost.
    val goHome: () -> Unit = {
        navController.navigate(Routes.ATTRACT) {
            popUpTo(Routes.ATTRACT) { inclusive = false }
            launchSingleTop = true
        }
    }

    InactivityHandler(
        timeoutMs = timeoutMs,
        warningMs = warningMs,
        enabled = timeoutEnabled,
        resetKey = currentRoute,
        onTimeout = goHome
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.ATTRACT
        ) {
            composable(Routes.ATTRACT) {
                AttractScreen(
                    onTap = {
                        val route = getStartRoute(settings)
                        navController.navigate(route)
                    },
                    onAdminLongPress = { navController.navigate(Routes.ADMIN) },
                    eventName = settings.eventName,
                    subtext = settings.attractSubtext
                )
            }

            composable(Routes.ADMIN) {
                val adminViewModel: AdminViewModel = hiltViewModel()
                AdminScreen(
                    onDismiss = { navController.popBackStack() },
                    onPrivacyPolicy = { navController.navigate(Routes.PRIVACY) },
                    onGallery = { navController.navigate(Routes.GALLERY) },
                    viewModel = adminViewModel
                )
            }

            composable(Routes.PRIVACY) {
                PrivacyPolicyScreen(
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(Routes.GALLERY) {
                val galleryViewModel: GalleryViewModel = hiltViewModel()
                GalleryScreen(
                    onPhotoSelected = { navController.popBackStack() },
                    onDismiss = { navController.popBackStack() },
                    viewModel = galleryViewModel
                )
            }

            composable(Routes.MODE_SELECT) {
                ModeSelectScreen(
                    onSinglePhoto = { navController.navigate(Routes.getReady(CaptureMode.Single)) },
                    onCollage = { navController.navigate(Routes.getReady(CaptureMode.Collage)) },
                    onGif = { navController.navigate(Routes.getReady(CaptureMode.Gif)) },
                    singlePhotoEnabled = settings.enableSinglePhotoMode,
                    collageEnabled = settings.enableCollageMode,
                    gifEnabled = settings.enableGifMode
                )
            }

            // Get Ready — live preview + instructions
            composable(
                route = Routes.GET_READY,
                arguments = listOf(navArgument("mode") { type = NavType.StringType })
            ) { backStackEntry ->
                val modeArg = backStackEntry.arguments?.getString("mode")
                val mode = CaptureMode.fromArg(modeArg)
                GetReadyScreen(
                    mode = mode,
                    onStart = { navController.navigate(Routes.capture(mode)) },
                    onBack = { navController.popBackStack(Routes.MODE_SELECT, inclusive = false) }
                )
            }

            // Capture — unified burst engine for all three modes
            composable(
                route = Routes.CAPTURE,
                arguments = listOf(navArgument("mode") { type = NavType.StringType })
            ) { backStackEntry ->
                val modeArg = backStackEntry.arguments?.getString("mode")
                val mode = CaptureMode.fromArg(modeArg)
                val captureViewModel: CaptureViewModel = hiltViewModel(backStackEntry)
                CaptureScreen(
                    mode = mode,
                    onDone = { navController.navigate(Routes.REVIEW) },
                    viewModel = captureViewModel
                )
            }

            composable(Routes.REVIEW) {
                // CAPTURE may have been popped out from under us if a navigate-
                // to-ATTRACT transition is in flight while this composable is
                // still being torn down. Bail without rendering; the transition
                // will dispose this entry shortly.
                val captureEntry = runCatching {
                    navController.getBackStackEntry(Routes.CAPTURE)
                }.getOrNull() ?: return@composable

                val captureViewModel: CaptureViewModel = hiltViewModel(captureEntry)
                val uiState by captureViewModel.uiState.collectAsState()
                val mode = uiState.mode
                val photos = uiState.photos

                ReviewScreen(
                    mode = mode,
                    photos = photos,
                    autoAcceptSeconds = settings.reviewAutoAcceptSeconds,
                    onRetake = {
                        captureViewModel.resetCapture()
                        navController.popBackStack(Routes.CAPTURE, inclusive = false)
                    },
                    onAccept = { pickedIndex ->
                        when (mode) {
                            CaptureMode.Single -> {
                                photos.getOrNull(pickedIndex)?.let { bmp ->
                                    captureViewModel.setActiveSinglePhoto(bmp)
                                }
                                navController.navigate(Routes.SHARE)
                            }
                            CaptureMode.Collage -> {
                                val assembled = CollageRenderer.render(photos, CollageLayout.GRID_2X2)
                                captureViewModel.setActiveSinglePhoto(assembled)
                                navController.navigate(Routes.SHARE)
                            }
                            CaptureMode.Gif -> {
                                photos.firstOrNull()?.let { bmp ->
                                    captureViewModel.setActiveSinglePhoto(bmp)
                                }
                                navController.navigate(Routes.SHARE)
                            }
                        }
                    }
                )
            }

            composable(Routes.SHARE) {
                // Same caveat as REVIEW: SHARE reaches into CAPTURE's
                // ViewModel for the captured photo, but during the goHome
                // popUpTo transition CAPTURE is popped before SHARE is
                // disposed, and getBackStackEntry will throw. Bail
                // gracefully; the disposal completes a frame later.
                val captureEntry = runCatching {
                    navController.getBackStackEntry(Routes.CAPTURE)
                }.getOrNull() ?: return@composable

                val captureViewModel: CaptureViewModel = hiltViewModel(captureEntry)
                val uiState by captureViewModel.uiState.collectAsState()

                ShareScreen(
                    photo = uiState.capturedPhoto,
                    onSessionEnd = goHome
                )
            }
        }
    }
}
