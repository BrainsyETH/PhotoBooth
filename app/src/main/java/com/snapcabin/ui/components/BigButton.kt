package com.snapcabin.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.snapcabin.ui.theme.CabinAccent
import com.snapcabin.ui.theme.CabinLineStrong
import com.snapcabin.ui.theme.CabinPrimary
import com.snapcabin.ui.theme.CabinSecondary
import com.snapcabin.ui.theme.CabinSurface
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Spacing
import com.snapcabin.ui.theme.Tap

enum class BigButtonVariant { Primary, Secondary, Accent, Surface }

@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: BigButtonVariant = BigButtonVariant.Primary,
    containerColor: Color? = null,
    contentColor: Color? = null,
    enabled: Boolean = true
) {
    val view = LocalView.current

    val (defaultContainer, defaultContent) = when (variant) {
        BigButtonVariant.Primary -> CabinPrimary to Color.White
        BigButtonVariant.Secondary -> CabinSecondary to Color.White
        BigButtonVariant.Accent -> CabinAccent to Espresso
        BigButtonVariant.Surface -> CabinSurface to Espresso
    }
    val resolvedContainer = containerColor ?: defaultContainer
    val resolvedContent = contentColor ?: defaultContent
    val border = if (variant == BigButtonVariant.Surface && containerColor == null) {
        BorderStroke(1.dp, CabinLineStrong)
    } else null

    Button(
        onClick = {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            onClick()
        },
        modifier = modifier.height(Tap.primary),
        enabled = enabled,
        shape = RoundedCornerShape(Radii.m),
        colors = ButtonDefaults.buttonColors(
            containerColor = resolvedContainer,
            contentColor = resolvedContent
        ),
        border = border,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
