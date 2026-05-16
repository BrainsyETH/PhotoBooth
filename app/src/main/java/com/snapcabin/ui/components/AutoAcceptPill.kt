package com.snapcabin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.Honey
import com.snapcabin.ui.theme.Radii

/**
 * Cream pill that floats over a photo while a countdown auto-accepts.
 * On API 31+ the backing surface picks up a soft blur via the spec's
 * shadow-2; below that the 94% cream + shadow alone reads cleanly.
 */
@Composable
fun AutoAcceptPill(
    secondsRemaining: Int,
    modifier: Modifier = Modifier,
    label: String = "Auto-accepting in ${secondsRemaining}s"
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(Radii.full), clip = false)
            .clip(RoundedCornerShape(Radii.full))
            .background(Cream.copy(alpha = 0.94f))
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Honey)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.04f.em,
                color = Espresso
            )
        )
    }
}
