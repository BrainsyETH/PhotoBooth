package com.snapcabin.ui.screens.unsupported

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapcabin.R
import com.snapcabin.ui.theme.CabinBackground
import com.snapcabin.ui.theme.CabinSurface
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.FrankRuhlLibre
import com.snapcabin.ui.theme.HankenGrotesk
import com.snapcabin.ui.theme.Oat
import com.snapcabin.ui.theme.Parchment
import com.snapcabin.ui.theme.Spacing
import com.snapcabin.ui.theme.Walnut

/**
 * Shown when the device's smallest-width is below 600dp. SnapCabin is a kiosk
 * app that depends on a real tablet form factor for everything from camera
 * framing to the side-by-side share layout; we surface this explicitly rather
 * than rendering a broken tablet layout on a phone.
 */
@Composable
fun UnsupportedDeviceScreen(smallestWidthDp: Int) {
    val backdrop = Brush.radialGradient(
        colors = listOf(CabinSurface, Parchment, Oat),
        radius = 1400f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CabinBackground)
            .background(backdrop)
            .padding(Spacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.snapcabin_logov2),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(160.dp)
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
            Text(
                text = "SnapCabin needs a tablet",
                fontSize = 28.sp,
                fontFamily = FrankRuhlLibre,
                fontWeight = FontWeight.Bold,
                color = Espresso,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.s))
            Text(
                text = "This device is too small for the booth layout. SnapCabin is built for Android tablets with a smallest-width of at least 600dp (about an 8-inch screen).",
                fontSize = 14.sp,
                fontFamily = HankenGrotesk,
                color = Walnut,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Detected smallest-width: ${smallestWidthDp}dp",
                fontSize = 12.sp,
                fontFamily = HankenGrotesk,
                color = Espresso.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
        }
    }
}
