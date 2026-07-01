package com.kevin.astra.presentation.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import kotlinx.coroutines.delay

private val bootLines = listOf(
    "> Initializing local inference engine",
    "> Loading edge model weights",
    "> Securing inference pipeline",
    "> Edge AI runtime ready  ✓",
)

@Composable
fun SplashScreen(
    contentPadding: PaddingValues,
    onFinished: () -> Unit,
) {
    var visibleLines by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(300)
        bootLines.indices.forEach { i ->
            visibleLines = i + 1
            delay(380)
        }
        delay(500)
        onFinished()
    }

    val infinite = rememberInfiniteTransition(label = "splash")

    val rotation by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "arc-rotation",
    )
    val counterRotation by infinite.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "counter-arc",
    )
    val ring1Pulse by infinite.animateFloat(
        initialValue = 0.07f, targetValue = 0.22f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "r1",
    )
    val ring2Pulse by infinite.animateFloat(
        initialValue = 0.14f, targetValue = 0.05f,
        animationSpec = infiniteRepeatable(tween(2100, delayMillis = 300), RepeatMode.Reverse),
        label = "r2",
    )
    val ring3Pulse by infinite.animateFloat(
        initialValue = 0.04f, targetValue = 0.16f,
        animationSpec = infiniteRepeatable(tween(1900, delayMillis = 700), RepeatMode.Reverse),
        label = "r3",
    )
    val corePulse by infinite.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "core",
    )
    val dotBlink by infinite.animateFloat(
        initialValue = 1f, targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "cursor",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AstraColors.Background)
            .padding(contentPadding),
    ) {
        // Neural dot grid backdrop
        NeuralDotGrid()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AstraSpacing.XL),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── Orbital ring system ───────────────────────────────────────
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Static pulsing rings
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2
                    drawCircle(
                        color = AstraColors.Primary.copy(alpha = ring3Pulse),
                        radius = r,
                        style = Stroke(0.8.dp.toPx()),
                    )
                    drawCircle(
                        color = AstraColors.Secondary.copy(alpha = ring2Pulse),
                        radius = r * 0.70f,
                        style = Stroke(0.8.dp.toPx()),
                    )
                    drawCircle(
                        color = AstraColors.Primary.copy(alpha = ring1Pulse),
                        radius = r * 0.43f,
                        style = Stroke(0.8.dp.toPx()),
                    )
                }

                // Rotating cyan arc (outer orbit)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = rotation },
                ) {
                    val diameter = size.minDimension
                    val topLeft = Offset.Zero
                    drawArc(
                        color = AstraColors.Secondary,
                        startAngle = -20f,
                        sweepAngle = 80f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                        alpha = 0.85f,
                    )
                    drawArc(
                        color = AstraColors.Secondary.copy(alpha = 0.25f),
                        startAngle = 170f,
                        sweepAngle = 30f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = 1.5f.dp.toPx(), cap = StrokeCap.Round),
                    )
                }

                // Counter-rotating blue arc (mid orbit)
                Canvas(
                    modifier = Modifier
                        .size(154.dp)
                        .graphicsLayer { rotationZ = counterRotation },
                ) {
                    val diameter = size.minDimension
                    val topLeft = Offset.Zero
                    drawArc(
                        color = AstraColors.Primary,
                        startAngle = 40f,
                        sweepAngle = 55f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = 1.5f.dp.toPx(), cap = StrokeCap.Round),
                        alpha = 0.7f,
                    )
                }

                // Glowing core dot
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .alpha(corePulse)
                        .background(
                            AstraColors.Primary.copy(alpha = 0.12f),
                            CircleShape,
                        )
                        .border(1.dp, AstraColors.Secondary.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(AstraColors.Secondary, CircleShape),
                    )
                }
            }

            Spacer(Modifier.height(AstraSpacing.XL))

            // ── Wordmark ──────────────────────────────────────────────────
            Text(
                text = "ASTRA",
                style = AstraTypography.DisplayLarge.copy(letterSpacing = 10.sp),
                color = AstraColors.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(AstraSpacing.XS))
            Text(
                text = "Edge AI · On-Device · Zero-Cloud",
                style = AstraTypography.Caption.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                ),
                color = AstraColors.Secondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(AstraSpacing.XXL))

            // ── Boot sequence ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AstraColors.Surface.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .border(1.dp, AstraColors.Border, RoundedCornerShape(16.dp))
                    .padding(AstraSpacing.M),
                verticalArrangement = Arrangement.spacedBy(AstraSpacing.XS),
            ) {
                bootLines.forEachIndexed { index, line ->
                    if (index < visibleLines) {
                        val isLast = index == visibleLines - 1
                        val isDone = index < visibleLines - 1
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = line,
                                style = AstraTypography.Metric,
                                color = if (isDone) AstraColors.TextSecondary else AstraColors.TextPrimary,
                            )
                            if (isLast && visibleLines < bootLines.size) {
                                Text(
                                    text = "█",
                                    style = AstraTypography.Metric,
                                    color = AstraColors.Secondary,
                                    modifier = Modifier.alpha(dotBlink),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NeuralDotGrid() {
    val infinite = rememberInfiniteTransition(label = "grid")
    val gridAlpha by infinite.animateFloat(
        initialValue = 0.03f, targetValue = 0.07f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "grid-alpha",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cols = 14
        val rows = 22
        val spacingX = size.width / cols
        val spacingY = size.height / rows
        for (col in 0..cols) {
            for (row in 0..rows) {
                drawCircle(
                    color = AstraColors.Primary.copy(alpha = gridAlpha),
                    radius = 1.5.dp.toPx(),
                    center = Offset(col * spacingX, row * spacingY),
                )
            }
        }
    }
}
