plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.snapcabin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.snapcabin"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val isReleaseBuild = gradle.startParameter.taskNames.any { name ->
                name.contains("Release", ignoreCase = true) ||
                    name.contains("bundleRelease", ignoreCase = true) ||
                    name.contains("assembleRelease", ignoreCase = true)
            }
            val storePass = System.getenv("KEYSTORE_PASSWORD")
            val alias = System.getenv("KEY_ALIAS")
            val keyPass = System.getenv("KEY_PASSWORD")
            if (isReleaseBuild) {
                require(!storePass.isNullOrBlank()) {
                    "KEYSTORE_PASSWORD env var is required for release builds. See docs/RELEASE_BUILD.md."
                }
                require(!alias.isNullOrBlank()) {
                    "KEY_ALIAS env var is required for release builds. See docs/RELEASE_BUILD.md."
                }
                require(!keyPass.isNullOrBlank()) {
                    "KEY_PASSWORD env var is required for release builds. See docs/RELEASE_BUILD.md."
                }
            }
            storeFile = file("keystore/release.keystore")
            storePassword = storePass ?: ""
            keyAlias = alias ?: ""
            keyPassword = keyPass ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Ensure consistent behavior across Samsung, Pixel, etc.
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    debugImplementation(libs.compose.ui.tooling)

    // AndroidX
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Splash Screen (Android 12+ compat)
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Browser — Custom Tabs for the in-app privacy policy link
    implementation(libs.androidx.browser)

    // Navigation
    implementation(libs.navigation.compose)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // ExifInterface for reading photo orientation
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // Image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)



    // QR code
    implementation(libs.zxing.core)

    // Unit tests (pure JVM — run with ./gradlew testDebugUnitTest)
    testImplementation("junit:junit:4.13.2")
}
