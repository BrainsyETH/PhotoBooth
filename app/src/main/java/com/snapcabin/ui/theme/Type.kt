package com.snapcabin.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.snapcabin.R

// Frank Ruhl Libre — warm serif display (no italic face; Compose synthesizes).
val FrankRuhlLibre = FontFamily(
    Font(R.font.frank_ruhl_libre, FontWeight.Light),
    Font(R.font.frank_ruhl_libre, FontWeight.Normal),
    Font(R.font.frank_ruhl_libre, FontWeight.Medium),
    Font(R.font.frank_ruhl_libre, FontWeight.SemiBold),
    Font(R.font.frank_ruhl_libre, FontWeight.Bold),
    Font(R.font.frank_ruhl_libre, FontWeight.ExtraBold),
    Font(R.font.frank_ruhl_libre, FontWeight.Black)
)

// Hanken Grotesk — humanist sans for UI
val HankenGrotesk = FontFamily(
    Font(R.font.hanken_grotesk, FontWeight.Light),
    Font(R.font.hanken_grotesk, FontWeight.Normal),
    Font(R.font.hanken_grotesk, FontWeight.Medium),
    Font(R.font.hanken_grotesk, FontWeight.SemiBold),
    Font(R.font.hanken_grotesk, FontWeight.Bold),
    Font(R.font.hanken_grotesk, FontWeight.ExtraBold),
    Font(R.font.hanken_grotesk_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.hanken_grotesk_italic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.hanken_grotesk_italic, FontWeight.SemiBold, FontStyle.Italic)
)

// Type scale tuned for a kiosk read from three feet away.
// Display tier uses Frank Ruhl Libre serif; UI tier uses Hanken Grotesk
// with generous tracking on uppercase labels and eyebrows.
val CabinTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FrankRuhlLibre,
        fontWeight = FontWeight.Bold,
        fontSize = 96.sp,
        lineHeight = 100.sp,
        letterSpacing = (-0.015f).em
    ),
    displayMedium = TextStyle(
        fontFamily = FrankRuhlLibre,
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,
        lineHeight = 68.sp,
        letterSpacing = (-0.015f).em
    ),
    displaySmall = TextStyle(
        fontFamily = FrankRuhlLibre,
        fontWeight = FontWeight.Medium,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.01f).em
    ),
    headlineLarge = TextStyle(
        fontFamily = FrankRuhlLibre,
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        lineHeight = 42.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    titleLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.16f.em
    ),
    bodyLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 34.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    bodySmall = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    labelLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.08f.em
    ),
    labelMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.12f.em
    ),
    labelSmall = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)
