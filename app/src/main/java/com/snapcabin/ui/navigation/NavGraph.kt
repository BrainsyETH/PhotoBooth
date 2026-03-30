package com.snapcabin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.snapcabin.settings.BoothSettings
import com.snapcabin.settings.SettingsManager
import com.snapcabin.ui.components.InactivityHandler
import com.snapcabin.ui.screens.admin.AdminScreen
import com.snapcabin.ui.screens.admin.AdminViewModel
import com.snapcabin.ui.screens.attract.AttractScreen
import com.snapcabin.ui.screens.branding.BrandingScreen
import com.snapcabin.ui.screens.branding.BrandingViewModel
import com.snapcabin.ui.screens.capture.CaptureScreen
import com.snapcabin.ui.screens.capture.CaptureViewModel
import com.snapcabin.ui.screens.collage.CollageScreen
import com.snapcabin.ui.screens.collage.CollageViewModel
import com.snapcabin.ui.screens.filters.FilterScreen
import com.snapcabin.ui.screens.filters.FilterViewModel
import com.snapcabin.ui.screens.gallery.GalleryScreen
import com.snapcabin.ui.screens.gallery.GalleryViewModel
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
    const val CAPTURE = "capture"
    const val REVIEW = "review"
    const val FILTER = "filter"
    const val BRANDING = "branding"
    const val SHARE = "share"
    const val COLLAGE = "collage"
    const val GIF = "gif"
    const val GIF_CAPTURE = "gif_capture"
    const val COLLAGE_CAPTURE = "collage_capture"
    const val ADMIN = "admin"
    const val PRIVACY = "privacy"
    const val GALLERY = "gallery"
    const val THANK_YOU = "thank_you"
}

private fun getScreenTimeout(route: String?): Long = when (route) {
    Routes.MODE_SELECT -> 30_000L
    Routes.CAPTURE, Routes.COLLAGE_CAPTURE, Routes.GIF_CAPTURE -> 90_000L
    Routes.REVIEW -> 30_000L
    Routes.FILTER, Routes.BRANDING -> 60_000L
    Routes.SHARE -> 60_000L
    Routes.COLLAGE, Routes.GIF -> 60_000L
    Routes.THANK_YOU -> 5_000L
    else -> 0L // Disabled for ATTRACT, ADMIN, PRIVACY, GALLERY
}

private fun getWarningDuration(route: String?): Long = when (route) {
    Routes.REVIEW -> 10_000L
    Routes.THANK_YOU -> 0L
    else -> 15_000L
}

private fun getStartRoute(settings: BoothSettings): String {
    val enabledModes = mutableListOf<String>()
    if (settings.enableSinglePhotoMode) enabledModes.add(Routes.CAPTURE)
    if (settings.enableCollageMode) enabledModes.add(Routes.COLLAGE)
    if (settings.enableGifMode) enabledModes.add(Routes.GIF)
    // Guard: if no modes enabled, fall back to mode select (shows all)
    if (enabledModes.isEmpty()) return Routes.MODE_SELECT
    return if (enabledModes.size == 1) enabledModes.first() else Routes.MODE_SELECT
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
        onTimeout = {
            navController.popBackStack(Routes.ATTRACT, inclusive = false)
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.ATTRACT
        ) {
            // Attract / idle screen
            composable(Routes.ATTRACT) {
                AttractScreen(
                    onTap = {
                        val route = getStartRoute(settings)
                        navController.navigate(route)
                    },
                    onAdminLongPress = { navController.navigate(Routes.ADMIN) }
                )
            }

            // Admin settings (pin-protected)
            composable(Routes.ADMIN) {
                val adminViewModel: AdminViewModel = hiltViewModel()
                AdminScreen(
                    onDismiss = { navController.popBackStack() },
                    onPrivacyPolicy = { navController.navigate(Routes.PRIVACY) },
                    onGallery = { navController.navigate(Routes.GALLERY) },
                    viewModel = adminViewModel
                )
            }

            // Privacy policy
            composable(Routes.PRIVACY) {
                PrivacyPolicyScreen(
                    onDismiss = { navController.popBackStack() }
                )
            }

            // Gallery (from admin)
            composable(Routes.GALLERY) {
                val galleryViewModel: GalleryViewModel = hiltViewModel()
                GalleryScreen(
                    onPhotoSelected = { navController.popBackStack() },
                    onDismiss = { navController.popBackStack() },
                    viewModel = galleryViewModel
                )
            }

            // Mode selection — passes enabled flags
            composable(Routes.MODE_SELECT) {
                ModeSelectScreen(
                    onSinglePhoto = { navController.navigate(Routes.CAPTURE) },
                    onCollage = { navController.navigate(Routes.COLLAGE) },
                    onGif = { navController.navigate(Routes.GIF) },
                    singlePhotoEnabled = settings.enableSinglePhotoMode,
                    collageEnabled = settings.enableCollageMode,
                    gifEnabled = settings.enableGifMode
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
                    autoAcceptSeconds = settings.reviewAutoAcceptSeconds,
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
                    onDone = { navController.navigate(Routes.BRANDING) },
                    viewModel = filterViewModel
                )
            }

            composable(Routes.BRANDING) {
                val captureEntry = navController.getBackStackEntry(Routes.CAPTURE)
                val captureViewModel: CaptureViewModel = hiltViewModel(captureEntry)
                val uiState by captureViewModel.uiState.collectAsState()
                val brandingViewModel: BrandingViewModel = hiltViewModel()

                BrandingScreen(
                    photo = uiState.capturedPhoto,
                    onDone = { navController.navigate(Routes.SHARE) },
                    onSkip = { navController.navigate(Routes.SHARE) },
                    viewModel = brandingViewModel
                )
            }

            // --- Share Screen (serves all flows) ---
            composable(Routes.SHARE) {
                // Determine which flow is active by checking backstack entries
                val backstack = navController.currentBackStack.collectAsState()
                val routes = backstack.value.mapNotNull { it.destination.route }.toSet()

                val hasCaptureRoute = Routes.CAPTURE in routes
                val hasCollageRoute = Routes.COLLAGE in routes
                val hasGifRoute = Routes.GIF in routes

                // Only call hiltViewModel for the flow that's actually on the backstack
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

            // Thank-you transition screen
            composable(Routes.THANK_YOU) {
                ThankYouScreen(
                    onDone = {
                        navController.popBackStack(Routes.ATTRACT, inclusive = false)
                    }
                )
            }

            // --- Collage Flow ---
            composable(Routes.COLLAGE) { backStackEntry ->
                val collageViewModel: CollageViewModel = hiltViewModel(backStackEntry)
                val captureEntry = try {
                    navController.getBackStackEntry(Routes.COLLAGE_CAPTURE)
                } catch (_: Exception) { null }

                val captureViewModel: CaptureViewModel? = captureEntry?.let { hiltViewModel(it) }
                val captureState = captureViewModel?.uiState?.collectAsState()

                val newPhoto = captureState?.value?.capturedPhoto
                if (newPhoto != null) {
                    collageViewModel.addPhoto(newPhoto)
                    captureViewModel.resetCapture()
                }

                CollageScreen(
                    initialPhoto = null,
                    onTakeMore = { navController.navigate(Routes.COLLAGE_CAPTURE) },
                    onDone = { _ -> navController.navigate(Routes.SHARE) },
                    onCancel = {
                        collageViewModel.reset()
                        navController.popBackStack(Routes.ATTRACT, inclusive = false)
                    },
                    viewModel = collageViewModel
                )
            }

            composable(Routes.COLLAGE_CAPTURE) { backStackEntry ->
                val captureViewModel: CaptureViewModel = hiltViewModel(backStackEntry)
                CaptureScreen(
                    onPhotoCaptured = { navController.popBackStack() },
                    viewModel = captureViewModel
                )
            }

            // --- GIF Flow (now routes through Share) ---
            composable(Routes.GIF) { backStackEntry ->
                val gifViewModel: GifViewModel = hiltViewModel(backStackEntry)
                val captureEntry = try {
                    navController.getBackStackEntry(Routes.GIF_CAPTURE)
                } catch (_: Exception) { null }

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
                    onDone = { _ -> navController.navigate(Routes.SHARE) },
                    onCancel = {
                        gifViewModel.reset()
                        navController.popBackStack(Routes.ATTRACT, inclusive = false)
                    },
                    viewModel = gifViewModel
                )
            }

            composable(Routes.GIF_CAPTURE) { backStackEntry ->
                val captureViewModel: CaptureViewModel = hiltViewModel(backStackEntry)
                CaptureScreen(
                    onPhotoCaptured = { navController.popBackStack() },
                    viewModel = captureViewModel
                )
            }
        }
    }
}
