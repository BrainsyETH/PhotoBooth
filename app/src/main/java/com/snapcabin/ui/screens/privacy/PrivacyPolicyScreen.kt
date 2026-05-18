package com.snapcabin.ui.screens.privacy

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.snapcabin.R
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.FrankRuhlLibre
import com.snapcabin.ui.theme.Spacing

@Composable
fun PrivacyPolicyScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val url = stringResource(R.string.privacy_view_online_url)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Spacing.xl)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.privacy_title),
            fontSize = 48.sp,
            fontFamily = FrankRuhlLibre,
            fontWeight = FontWeight.Medium,
            color = Espresso
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = stringResource(R.string.privacy_body),
            style = MaterialTheme.typography.bodyLarge,
            color = Espresso.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.md)
        ) {
            BigButton(
                text = stringResource(R.string.privacy_view_online),
                onClick = {
                    val uri = url.toUri()
                    val tabs = CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .build()
                    try {
                        tabs.launchUrl(context, uri)
                    } catch (_: ActivityNotFoundException) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        } catch (_: ActivityNotFoundException) {
                            // No browser installed — silently no-op; the in-app summary is the fallback.
                        }
                    }
                },
                variant = BigButtonVariant.Primary
            )
            BigButton(
                text = "CLOSE",
                onClick = onDismiss,
                variant = BigButtonVariant.Secondary
            )
        }
    }
}
