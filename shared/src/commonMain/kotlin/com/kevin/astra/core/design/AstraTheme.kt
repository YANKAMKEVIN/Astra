package com.kevin.astra.core.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin.astra.domain.settings.ThemeHolder

// ── Palette data ──────────────────────────────────────────────────────────────

data class AstraColorPalette(
    val Primary: Color,
    val Secondary: Color,
    val Success: Color,
    val Warning: Color,
    val Error: Color,
    val Background: Color,
    val Surface: Color,
    val SurfaceElevated: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val TextDisabled: Color,
    val Border: Color,
)

private val DarkPalette = AstraColorPalette(
    Primary = Color(0xFF3B82F6),
    Secondary = Color(0xFF22D3EE),
    Success = Color(0xFF22C55E),
    Warning = Color(0xFFF59E0B),
    Error = Color(0xFFEF4444),
    Background = Color(0xFF0B1220),
    Surface = Color(0xFF111827),
    SurfaceElevated = Color(0xFF1F2937),
    TextPrimary = Color(0xFFF9FAFB),
    TextSecondary = Color(0xFF9CA3AF),
    TextDisabled = Color(0xFF6B7280),
    Border = Color(0xFF273449),
)

private val LightPalette = AstraColorPalette(
    Primary = Color(0xFF1D6FDB),
    Secondary = Color(0xFF0891B2),
    Success = Color(0xFF16A34A),
    Warning = Color(0xFFD97706),
    Error = Color(0xFFDC2626),
    Background = Color(0xFFF1F5F9),
    Surface = Color(0xFFFFFFFF),
    SurfaceElevated = Color(0xFFE2E8F0),
    TextPrimary = Color(0xFF0F172A),
    TextSecondary = Color(0xFF475569),
    TextDisabled = Color(0xFF94A3B8),
    Border = Color(0xFFCBD5E1),
)

// ── Reactive color singleton ──────────────────────────────────────────────────

private var _palette by mutableStateOf(DarkPalette)

object AstraColors {
    val Primary: Color get() = _palette.Primary
    val Secondary: Color get() = _palette.Secondary
    val Success: Color get() = _palette.Success
    val Warning: Color get() = _palette.Warning
    val Error: Color get() = _palette.Error
    val Background: Color get() = _palette.Background
    val Surface: Color get() = _palette.Surface
    val SurfaceElevated: Color get() = _palette.SurfaceElevated
    val TextPrimary: Color get() = _palette.TextPrimary
    val TextSecondary: Color get() = _palette.TextSecondary
    val TextDisabled: Color get() = _palette.TextDisabled
    val Border: Color get() = _palette.Border

    internal fun applyPalette(p: AstraColorPalette) { _palette = p }
}

// ── Spacing ───────────────────────────────────────────────────────────────────

object AstraSpacing {
    val XS = 4.dp
    val S = 8.dp
    val M = 16.dp
    val L = 24.dp
    val XL = 32.dp
    val XXL = 48.dp
}

// ── Typography ────────────────────────────────────────────────────────────────

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

// ── Material color schemes ────────────────────────────────────────────────────

private fun darkColorScheme(p: AstraColorPalette): ColorScheme = darkColorScheme(
    primary = p.Primary,
    onPrimary = p.TextPrimary,
    secondary = p.Secondary,
    onSecondary = p.Background,
    tertiary = p.Success,
    error = p.Error,
    background = p.Background,
    onBackground = p.TextPrimary,
    surface = p.Surface,
    onSurface = p.TextPrimary,
    surfaceVariant = p.SurfaceElevated,
    onSurfaceVariant = p.TextSecondary,
    outline = p.Border,
)

private fun lightColorScheme(p: AstraColorPalette): ColorScheme = lightColorScheme(
    primary = p.Primary,
    onPrimary = Color.White,
    secondary = p.Secondary,
    onSecondary = Color.White,
    tertiary = p.Success,
    error = p.Error,
    background = p.Background,
    onBackground = p.TextPrimary,
    surface = p.Surface,
    onSurface = p.TextPrimary,
    surfaceVariant = p.SurfaceElevated,
    onSurfaceVariant = p.TextSecondary,
    outline = p.Border,
)

private val MaterialAstraTypography = Typography(
    displayLarge = AstraTypography.DisplayLarge,
    headlineLarge = AstraTypography.Headline,
    titleLarge = AstraTypography.Title,
    bodyLarge = AstraTypography.Body,
    labelMedium = AstraTypography.Caption,
)

// ── Theme composable ──────────────────────────────────────────────────────────

@Composable
fun AstraTheme(content: @Composable () -> Unit) {
    val isLight by ThemeHolder.isLight.collectAsState()
    val palette = if (isLight) LightPalette else DarkPalette
    SideEffect { AstraColors.applyPalette(palette) }
    MaterialTheme(
        colorScheme = if (isLight) lightColorScheme(palette) else darkColorScheme(palette),
        typography = MaterialAstraTypography,
        content = content,
    )
}
