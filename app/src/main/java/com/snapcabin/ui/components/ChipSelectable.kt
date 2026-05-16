package com.snapcabin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.snapcabin.ui.theme.CabinLineStrong
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.Honey
import com.snapcabin.ui.theme.Pine
import com.snapcabin.ui.theme.Radii

/**
 * Selectable chip / row. Two visual flavors:
 *   - Pill = compact overlay chip (20dp radius)
 *   - Tile = wider row for layouts / templates (12dp radius)
 *
 * Selected: Pine background, White text, Pine outer border, Honey inset border.
 */
enum class ChipShape { Pill, Tile }

@Composable
fun ChipSelectable(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: ChipShape = ChipShape.Pill
) {
    val radius: Dp = when (shape) {
        ChipShape.Pill -> Radii.l
        ChipShape.Tile -> Radii.s
    }
    val padH = 18.dp
    val padV = if (shape == ChipShape.Pill) 10.dp else 14.dp
    val insetWidth: Dp = if (shape == ChipShape.Pill) 1.dp else 2.dp

    val container = if (selected) Pine else Cream
    val outlineColor = if (selected) Pine else CabinLineStrong
    val contentColor = if (selected) Color.White else Espresso

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(container)
            .border(BorderStroke(1.dp, outlineColor), RoundedCornerShape(radius))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Inset honey border (drawn inside outer border for the selected state).
        if (selected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(2.dp)
                    .border(
                        BorderStroke(insetWidth, Honey),
                        RoundedCornerShape(radius - 2.dp)
                    )
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = padH, vertical = padV)
        )
    }
}
