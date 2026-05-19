package com.snapcabin.ui.screens.admin.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.screens.admin.SettingRow
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.Spacing

@Composable
internal fun ToolsSection(onGallery: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        BigButton(
            text = "PHOTO GALLERY",
            onClick = onGallery,
            variant = BigButtonVariant.Primary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
internal fun AboutSection(onPrivacyPolicy: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        BigButton(
            text = "PRIVACY POLICY",
            onClick = onPrivacyPolicy,
            variant = BigButtonVariant.Surface,
            modifier = Modifier.fillMaxWidth()
        )

        SettingRow("Version") {
            Text(
                text = "1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = Espresso.copy(alpha = 0.72f)
            )
        }
    }
}
