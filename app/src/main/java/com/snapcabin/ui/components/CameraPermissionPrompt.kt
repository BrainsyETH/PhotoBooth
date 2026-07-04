package com.snapcabin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapcabin.R
import com.snapcabin.ui.theme.FrankRuhlLibre
import com.snapcabin.ui.theme.HankenGrotesk
import com.snapcabin.ui.theme.Spacing

/**
 * Shown over the (black) camera surface when CAMERA hasn't been granted.
 * A plain "permission required" label was a dead end — if the guest (or host)
 * dismissed the system dialog, the booth just sat there. This gives one big,
 * obvious way to ask again.
 */
@Composable
fun CameraPermissionPrompt(
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = modifier
            .widthIn(max = 560.dp)
            .padding(Spacing.xl)
    ) {
        Text(
            text = stringResource(R.string.capture_permission_required),
            fontFamily = FrankRuhlLibre,
            fontWeight = FontWeight.SemiBold,
            fontSize = 36.sp,
            lineHeight = 42.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.capture_permission_body),
            fontFamily = HankenGrotesk,
            fontSize = 18.sp,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        BigButton(
            text = stringResource(R.string.capture_permission_button),
            onClick = onRequest,
            variant = BigButtonVariant.Primary
        )
    }
}
