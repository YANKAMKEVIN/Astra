package com.kevin.astra.core.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AstraColors {
    val Primary = Color(0xFF3B82F6)
    val Secondary = Color(0xFF22D3EE)
    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Background = Color(0xFF0B1220)
    val Surface = Color(0xFF111827)
    val SurfaceElevated = Color(0xFF1F2937)
    val TextPrimary = Color(0xFFF9FAFB)
    val TextSecondary = Color(0xFF9CA3AF)
    val TextDisabled = Color(0xFF6B7280)
    val Border = Color(0xFF273449)
}

object AstraSpacing {
    val XS = 4.dp
    val S = 8.dp
    val M = 16.dp
    val L = 24.dp
    val XL = 32.dp
    val XXL = 48.dp
}

object AstraTypography {
    val DisplayLarge = TextStyle(
        fontSize = 40.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val Headline = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val Title = TextStyle(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium,
    )
    val Body = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )
    val Caption = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
    val Metric = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    )
}

private val AstraColorScheme: ColorScheme = darkColorScheme(
    primary = AstraColors.Primary,
    onPrimary = AstraColors.TextPrimary,
    secondary = AstraColors.Secondary,
    onSecondary = AstraColors.Background,
    tertiary = AstraColors.Success,
    error = AstraColors.Error,
    background = AstraColors.Background,
    onBackground = AstraColors.TextPrimary,
    surface = AstraColors.Surface,
    onSurface = AstraColors.TextPrimary,
    surfaceVariant = AstraColors.SurfaceElevated,
    onSurfaceVariant = AstraColors.TextSecondary,
    outline = AstraColors.Border,
)

private val MaterialAstraTypography = Typography(
    displayLarge = AstraTypography.DisplayLarge,
    headlineLarge = AstraTypography.Headline,
    titleLarge = AstraTypography.Title,
    bodyLarge = AstraTypography.Body,
    labelMedium = AstraTypography.Caption,
)

@Composable
fun AstraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AstraColorScheme,
        typography = MaterialAstraTypography,
        content = content,
    )
}
