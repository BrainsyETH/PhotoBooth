package com.snapcabin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.HankenGrotesk
import com.snapcabin.ui.theme.Honey
import com.snapcabin.ui.theme.Oat
import com.snapcabin.ui.theme.Pine
import com.snapcabin.ui.theme.Walnut

enum class IndicatorMode { Single, Collage, Gif }

@Composable
fun ShotIndicator(
    mode: IndicatorMode,
    shotCount: Int,
    totalShots: Int,
    kindLabel: String,
    modifier: Modifier = Modifier
) {
    val eyebrowText = when (mode) {
        IndicatorMode.Gif -> "$kindLabel · $shotCount / $totalShots FRAMES"
        IndicatorMode.Single -> {
            val displayShot = if (shotCount < totalShots) shotCount + 1 else totalShots
            "$kindLabel · $displayShot / $totalShots"
        }
        IndicatorMode.Collage -> "$kindLabel · $shotCount / $totalShots"
    }

    Column(
        modifier = modifier
            .widthIn(min = 132.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Cream.copy(alpha = 0.94f))
            .padding(14.dp)
    ) {
        Text(
            text = eyebrowText.uppercase(),
            fontFamily = HankenGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.22f.em,
            color = Walnut
        )

        Spacer(modifier = Modifier.height(10.dp))

        when (mode) {
            IndicatorMode.Single -> SingleDots(shotCount, totalShots)
            IndicatorMode.Collage -> CollageGrid(shotCount, totalShots)
            IndicatorMode.Gif -> GifProgress(shotCount, totalShots)
        }
    }
}

@Composable
private fun SingleDots(shotCount: Int, totalShots: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0 until totalShots) {
            val filled = i < shotCount
            val isNext = i == shotCount && shotCount < totalShots
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (filled) Pine else Oat)
                    .border(
                        width = if (isNext) 2.dp else 1.dp,
                        color = if (isNext) Honey else CabinLine,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun CollageGrid(shotCount: Int, totalShots: Int) {
    Column(
        modifier = Modifier.width(96.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CollageCell(0, shotCount, totalShots)
            CollageCell(1, shotCount, totalShots)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CollageCell(2, shotCount, totalShots)
            CollageCell(3, shotCount, totalShots)
        }
    }
}

@Composable
private fun CollageCell(i: Int, shotCount: Int, totalShots: Int) {
    val filled = i < shotCount
    val isNext = i == shotCount && shotCount < totalShots
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (filled) Pine else Oat)
            .border(
                width = if (isNext) 2.dp else 1.dp,
                color = if (isNext) Honey else CabinLine,
                shape = RoundedCornerShape(4.dp)
            )
    )
}

@Composable
private fun GifProgress(shotCount: Int, totalShots: Int) {
    val fraction = if (totalShots <= 0) 0f else (shotCount.toFloat() / totalShots).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Oat)
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Pine)
            )
        }
    }
}
