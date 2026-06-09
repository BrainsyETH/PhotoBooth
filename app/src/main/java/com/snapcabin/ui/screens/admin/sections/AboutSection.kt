package com.snapcabin.ui.screens.admin.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.screens.admin.SettingRow
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.Spacing

@Composable
internal fun AboutSection(
    onGallery: () -> Unit,
    onPrivacyPolicy: () -> Unit
) {
    val context = LocalContext.current
    // Read from the installed package rather than a hardcoded string, so the
    // displayed version can never drift from the build.
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        BigButton(
            text = "PHOTO GALLERY",
            onClick = onGallery,
            variant = BigButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth()
        )

        BigButton(
            text = "PRIVACY POLICY",
            onClick = onPrivacyPolicy,
            variant = BigButtonVariant.Surface,
            modifier = Modifier.fillMaxWidth()
        )

        SettingRow("Version") {
            Text(
                text = versionName,
                style = MaterialTheme.typography.bodyMedium,
                color = Espresso.copy(alpha = 0.72f)
            )
        }
    }
}
