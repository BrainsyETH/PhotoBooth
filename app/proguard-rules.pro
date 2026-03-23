# PhotoBooth ProGuard Rules

# --- Hilt / Dagger ---
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn dagger.hilt.**

# --- Compose ---
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# --- CameraX ---
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# --- Coil ---
-keep class coil3.** { *; }
-dontwarn coil3.**

# --- NanoHTTPD ---
-keep class fi.iki.elonen.** { *; }

# --- ZXing ---
-keep class com.google.zxing.** { *; }

# --- DataStore ---
-keep class androidx.datastore.** { *; }

# --- Kotlin Serialization / Coroutines ---
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# --- App models (keep data classes used in StateFlow) ---
-keep class com.photobooth.settings.BoothSettings { *; }
-keep class com.photobooth.settings.PhotoResolution { *; }
-keep class com.photobooth.camera.DetectedCamera { *; }
-keep class com.photobooth.ui.screens.admin.CameraInfo { *; }

# --- Prevent R8 from stripping lifecycle observers ---
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# --- Keep Hilt entry points ---
-keep class * extends android.app.Application { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }

# --- Prevent stripping of Camera2 characteristics constants ---
-keep class android.hardware.camera2.CameraCharacteristics { *; }
