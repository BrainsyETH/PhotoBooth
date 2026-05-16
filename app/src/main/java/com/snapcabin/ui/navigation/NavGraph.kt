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
import com.snapcabin.ui.screens.collage.CollageScreen
import com.snapcabin.ui.screens.collage.CollageViewModel
import com.snapcabin.ui.screens.filters.FilterScreen
import com.snapcabin.ui.screens.filters.FilterViewModel
import com.snapcabin.ui.screens.gallery.GalleryScreen
import com.snapcabin.ui.screens.gallery.GalleryViewModel
import com.snapcabin.ui.screens.getready.GetReadyScreen
import com.snapcabin.ui.screens.gif.GifScreen
import com.snapcabin.ui.screens.gif.GifViewModel
import com.snapcabin.ui.screens.modeselect.ModeSelectScreen
import com.snapcabin.ui.screens.privacy.PrivacyPolicyScreen
import com.snapcabin.ui.screens.review.ReviewScreen
import com.snapcabin.ui.screens.share.ShareScreen
import com.snapcabin.ui.screens.thankyou.ThankYouScreen

object Routes {
    const val ATTRACT = "attract"
    const val MODE_SELECT = "mode_select"
    const val GET_READY = "get_ready/{mode}"
    const val CAPTURE = "capture/{mode}"
    const val REVIEW = "review"
    const val FILTER = "filter"
    const val SHARE = "share"
    // LEGACY: kept for potential admin access
    const val COLLAGE = "collage"
    // LEGACY: kept for potential admin access
    const val GIF = "gif"
    const val ADMIN = "admin"
    const val PRIVACY = "privacy"
    const val GALLERY = "gallery"
    const val THANK_YOU = "thank_you"

    fun getReady(mode: CaptureMode) = "get_ready/${mode.routeArg}"
    fun capture(mode: CaptureMode) = "capture/${mode.routeArg}"
}

private fun getScreenTimeout(route: String?): Long = when {
    route == Routes.MODE_SELECT -> 30_000L
    route?.startsWith("get_ready") == true -> 60_000L
    route?.startsWith("capture") == true -> 90_000L
    route == Routes.REVIEW -> 30_000L
    route == Routes.FILTER -> 60_000L
    route == Routes.SHARE -> 60_000L
    route == Routes.COLLAGE || route == Routes.GIF -> 60_000L
    route == Routes.THANK_YOU -> 5_000L
    else -> 0L
}

private fun getWarningDuration(route: String?): Long = when (route) {
    Routes.REVIEW -> 10_000L
    Routes.THANK_YOU -> 0L
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

    InactivityHandler(
        timeoutMs = timeoutMs,
        warningMs = warningMs,
        enabled = timeoutEnabled,
        resetKey = currentRoute,
        onTimeout = {
            navController.popBackStack(Routes.ATTRACT, inclusive = false)
        }
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
                    onAdminLongPress = { navController.navigate(Routes.ADMIN) }
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
                val captureEntry = navController.getBackStackEntry(Routes.CAPTURE)
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
                                navController.navigate(Routes.FILTER)
                            }
                            CaptureMode.Collage -> {
                                val assembled = CollageRenderer.render(photos, CollageLayout.GRID_2X2)
                                captureViewModel.setActiveSinglePhoto(assembled)
                                navController.navigate(Routes.FILTER)
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
                val backstack = navController.currentBackStack.collectAsState()
                val routes = backstack.value.mapNotNull { it.destination.route }.toSet()

                val hasCaptureRoute = Routes.CAPTURE in routes
                val hasCollageRoute = Routes.COLLAGE in routes
                val hasGifRoute = Routes.GIF in routes

                val singlePhoto = if (hasCaptureRoute) {
                    val vm: CaptureViewModel = hiltViewModel(
                        navController.getBackStackEntry(Routes.CAPTURE)
                    )
                    val state by vm.uiState.collectAsState()
                    state.capturedPhoto
                } else null

                val collagePhoto = if (hasCollageRoute) {
                    val vm: CollageViewModel = hiltViewModel(
                        navController.getBackStackEntry(Routes.COLLAGE)
                    )
                    val state by vm.uiState.collectAsState()
                    state.previewBitmap
                } else null

                val gifPreview = if (hasGifRoute) {
                    val vm: GifViewModel = hiltViewModel(
                        navController.getBackStackEntry(Routes.GIF)
                    )
                    val state by vm.uiState.collectAsState()
                    state.frames.firstOrNull()
                } else null

                val photo = singlePhoto ?: collagePhoto ?: gifPreview

                ShareScreen(
                    photo = photo,
                    onDone = {
                        navController.navigate(Routes.THANK_YOU) {
                            popUpTo(Routes.ATTRACT) { inclusive = false }
                        }
                    }
                )
            }

            composable(Routes.THANK_YOU) {
                ThankYouScreen(
                    onDone = {
                        navController.popBackStack(Routes.ATTRACT, inclusive = false)
                    }
                )
            }

            // LEGACY: kept for potential admin access
            composable(Routes.COLLAGE) { backStackEntry ->
                val collageViewModel: CollageViewModel = hiltViewModel(backStackEntry)
                CollageScreen(
                    initialPhoto = null,
                    onTakeMore = { navController.navigate(Routes.MODE_SELECT) },
                    onDone = { _ -> navController.navigate(Routes.SHARE) },
                    onCancel = {
                        collageViewModel.reset()
                        navController.popBackStack(Routes.ATTRACT, inclusive = false)
                    },
                    viewModel = collageViewModel
                )
            }

            // LEGACY: kept for potential admin access
            composable(Routes.GIF) { backStackEntry ->
                val gifViewModel: GifViewModel = hiltViewModel(backStackEntry)
                GifScreen(
                    initialFrame = null,
                    onTakeMore = { navController.navigate(Routes.MODE_SELECT) },
                    onDone = { _ -> navController.navigate(Routes.SHARE) },
                    onCancel = {
                        gifViewModel.reset()
                        navController.popBackStack(Routes.ATTRACT, inclusive = false)
                    },
                    viewModel = gifViewModel
                )
            }
        }
    }
}
