package com.kevin.astra.core.design

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kevin.astra.core.navigation.AstraDestination

enum class AstraButtonStyle {
    Primary,
    Secondary,
    Danger,
    Ghost,
}

@Composable
fun AstraCard(
    title: String,
    subtitle: String? = null,
    status: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AstraColors.Surface),
        border = BorderStroke(1.dp, AstraColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(AstraSpacing.L)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = AstraTypography.Title,
                        color = AstraColors.TextPrimary,
                    )
                    if (subtitle != null) {
                        Spacer(Modifier.height(AstraSpacing.XS))
                        Text(
                            text = subtitle,
                            style = AstraTypography.Caption,
                            color = AstraColors.TextSecondary,
                        )
                    }
                }
                if (status != null) {
                    AstraChip(label = status, color = AstraColors.Secondary)
                }
            }
            content()
        }
    }
}

/**
 * ASTRA Core — the signature "living" orb: a Primary→Secondary gradient core
 * inside two ultra-subtle rings over a soft radial halo, with a very slow
 * breathing + halo pulse. No backdrop blur required. Shared across screens
 * (Chat empty state, Home hero, …).
 */
@Composable
fun AstraCore(coreSize: Dp = 96.dp, animated: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "astra-core")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (animated) 1.035f else 1f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "core-scale",
    )
    val halo by transition.animateFloat(
        initialValue = if (animated) 0.07f else 0.10f,
        targetValue = if (animated) 0.14f else 0.10f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "core-halo",
    )
    Box(
        modifier = Modifier.size(coreSize * 1.9f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        listOf(AstraColors.Secondary.copy(alpha = halo), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .size(coreSize * 1.48f)
                .border(1.dp, AstraColors.Primary.copy(alpha = 0.10f), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(coreSize * 1.23f)
                .border(1.dp, AstraColors.Secondary.copy(alpha = 0.16f), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(coreSize)
                .scale(scale)
                .shadow(
                    elevation = 26.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = AstraColors.Primary,
                    spotColor = AstraColors.Secondary,
                )
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(AstraColors.Primary, AstraColors.Secondary)))
                .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✦", fontSize = (coreSize.value * 0.36f).sp, color = Color.White)
        }
    }
}

/** Signature CTA: a full-width Primary→Secondary gradient button. */
@Composable
fun AstraGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (enabled) {
                    Modifier.background(
                        Brush.linearGradient(listOf(AstraColors.Primary, AstraColors.Secondary)),
                    )
                } else {
                    Modifier.background(AstraColors.SurfaceElevated)
                },
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AstraTypography.Body,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Minimal line chart (no dependency) for a small series of values. */
@Composable
fun AstraSparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = AstraColors.Secondary,
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val maxV = values.max()
        val minV = values.min()
        val range = (maxV - minV).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        val fill = Path()
        val line = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - minV) / range) * (size.height * 0.9f) - size.height * 0.05f
            if (i == 0) {
                line.moveTo(x, y)
                fill.moveTo(x, size.height)
                fill.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(size.width, size.height)
        fill.close()
        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                listOf(lineColor.copy(alpha = 0.22f), Color.Transparent),
            ),
        )
        drawPath(
            path = line,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
fun AstraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AstraButtonStyle = AstraButtonStyle.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val containerColor = when (style) {
        AstraButtonStyle.Primary -> AstraColors.Primary
        AstraButtonStyle.Secondary -> AstraColors.SurfaceElevated
        AstraButtonStyle.Danger -> AstraColors.Error
        AstraButtonStyle.Ghost -> Color.Transparent
    }

    if (style == AstraButtonStyle.Ghost) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, AstraColors.Border),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AstraColors.TextPrimary,
                disabledContentColor = AstraColors.TextDisabled,
            ),
        ) {
            AstraButtonContent(text, leadingIcon)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = AstraColors.TextPrimary,
                disabledContainerColor = AstraColors.SurfaceElevated,
                disabledContentColor = AstraColors.TextDisabled,
            ),
        ) {
            AstraButtonContent(text, leadingIcon)
        }
    }
}

@Composable
private fun AstraButtonContent(text: String, leadingIcon: ImageVector?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
    ) {
        if (leadingIcon != null) {
            AstraIcon(icon = leadingIcon, tint = LocalContentColor.current, size = 18.dp)
        }
        Text(text)
    }
}

@Composable
fun AstraChip(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = AstraColors.Primary,
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.38f), RoundedCornerShape(12.dp))
            .padding(horizontal = AstraSpacing.S, vertical = AstraSpacing.XS),
    ) {
        Text(
            text = label,
            style = AstraTypography.Caption,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun AstraMetricCard(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(AstraColors.SurfaceElevated, RoundedCornerShape(16.dp))
            .border(1.dp, AstraColors.Border, RoundedCornerShape(16.dp))
            .padding(AstraSpacing.M),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = AstraTypography.Title,
                color = AstraColors.TextPrimary,
            )
            if (unit.isNotBlank()) {
                Text(
                    text = " $unit",
                    style = AstraTypography.Metric,
                    color = AstraColors.Secondary,
                )
            }
        }
        Spacer(Modifier.height(AstraSpacing.XS))
        Text(
            text = label,
            style = AstraTypography.Caption,
            color = AstraColors.TextSecondary,
        )
    }
}

@Composable
fun AstraErrorView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AstraColors.Error.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .border(1.dp, AstraColors.Error.copy(alpha = 0.24f), RoundedCornerShape(24.dp))
            .padding(AstraSpacing.L),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = AstraTypography.Title,
            color = AstraColors.Error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AstraSpacing.S))
        Text(
            text = message,
            style = AstraTypography.Body,
            color = AstraColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(AstraSpacing.M))
            AstraButton(
                text = "Retry",
                onClick = onRetry,
                style = AstraButtonStyle.Danger,
            )
        }
    }
}

@Composable
fun AstraEmptyView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AstraColors.SurfaceElevated, RoundedCornerShape(24.dp))
            .border(1.dp, AstraColors.Border, RoundedCornerShape(24.dp))
            .padding(AstraSpacing.L),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(AstraColors.Secondary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(AstraColors.Secondary, RoundedCornerShape(50)),
            )
        }
        Spacer(Modifier.height(AstraSpacing.M))
        Text(
            text = title,
            style = AstraTypography.Title,
            color = AstraColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AstraSpacing.S))
        Text(
            text = message,
            style = AstraTypography.Body,
            color = AstraColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AstraDemoModeIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(AstraColors.Secondary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .border(1.dp, AstraColors.Secondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = AstraSpacing.S, vertical = AstraSpacing.XS),
    ) {
        Text(
            text = "Offline Demo Mode",
            style = AstraTypography.Caption,
            color = AstraColors.Secondary,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Floating glass dock surface color (approx. #152033 translucent). */
private val DockGlass = Color(0xFF152033)

/**
 * Reusable floating "frosted glass" bottom sheet: a translucent, suspended
 * panel (deep scrim + sheen + white edge + drop shadow) with a custom drag
 * handle and a title. Content is laid out in a Column with S spacing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstraGlassSheet(
    title: String,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AstraSpacing.S)
                .navigationBarsPadding()
                .padding(bottom = AstraSpacing.S)
                .shadow(30.dp, RoundedCornerShape(32.dp), clip = false)
                .clip(RoundedCornerShape(32.dp))
                .background(AstraColors.SurfaceElevated.copy(alpha = 0.94f))
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(32.dp))
                .padding(horizontal = AstraSpacing.L)
                .padding(top = AstraSpacing.M, bottom = AstraSpacing.L),
            verticalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AstraColors.TextSecondary.copy(alpha = 0.35f)),
            )
            Spacer(Modifier.height(AstraSpacing.XS))
            Text(
                text = title,
                style = AstraTypography.Title,
                color = AstraColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AstraTypography.Caption,
                    color = AstraColors.TextSecondary,
                )
            }
            Spacer(Modifier.height(AstraSpacing.XS))
            content()
        }
    }
}

/** A frosted-glass row used inside sheets: icon-chip + label + trailing chevron. */
@Composable
fun AstraGlassRow(
    glyph: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String = "›",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AstraColors.Surface.copy(alpha = 0.6f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = AstraSpacing.M, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.M),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AstraColors.Secondary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = glyph, style = AstraTypography.Body)
        }
        Text(
            text = label,
            style = AstraTypography.Body,
            color = AstraColors.TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (trailing.isNotEmpty()) {
            Text(text = trailing, style = AstraTypography.Title, color = AstraColors.TextSecondary)
        }
    }
}

@Composable
fun AstraNavigationBar(
    selectedDestination: AstraDestination,
    onDestinationSelected: (AstraDestination) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(20.dp, RoundedCornerShape(28.dp), clip = false)
                .clip(RoundedCornerShape(28.dp))
                .background(DockGlass.copy(alpha = 0.92f))
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(28.dp))
                .padding(horizontal = AstraSpacing.S),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AstraDestination.primaryNavDestinations.forEach { destination ->
                NavBarItem(
                    icon = navIconFor(destination),
                    label = destination.shortLabel,
                    selected = destination == selectedDestination,
                    onClick = { onDestinationSelected(destination) },
                )
            }
        }
    }
}

private fun navIconFor(destination: AstraDestination): ImageVector = when (destination) {
    AstraDestination.ProjectOverview -> AstraIcons.Home
    AstraDestination.Assistant -> AstraIcons.Sparkle
    AstraDestination.Documents -> AstraIcons.Article
    AstraDestination.Benchmark -> AstraIcons.BarChart
    AstraDestination.Settings -> AstraIcons.Tune
    else -> AstraIcons.Sparkle
}

@Composable
private fun NavBarItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(
                color = if (selected) AstraColors.Primary.copy(alpha = 0.18f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AstraIcon(
            icon = icon,
            tint = if (selected) AstraColors.Secondary else AstraColors.TextSecondary,
            size = 22.dp,
        )
        Spacer(Modifier.height(AstraSpacing.XS))
        Text(
            text = label,
            style = AstraTypography.Caption.copy(fontSize = 11.sp),
            color = if (selected) AstraColors.TextPrimary else AstraColors.TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
fun DemoModeBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AstraColors.Warning.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .border(1.dp, AstraColors.Warning.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "⚡ OFFLINE DEMO MODE — responses are simulated",
            style = AstraTypography.Caption,
            color = AstraColors.Warning,
            textAlign = TextAlign.Center,
        )
    }
}
