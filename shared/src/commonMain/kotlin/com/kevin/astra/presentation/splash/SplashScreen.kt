package com.kevin.astra.presentation.splash

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    contentPadding: PaddingValues,
    onFinished: () -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(2400)
        onFinished()
    }

    val infinite = rememberInfiniteTransition(label = "splash")
    val markPulse by infinite.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "mark-pulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AstraColors.Background)
            .padding(contentPadding),
    ) {
        SplashBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AstraSpacing.XL),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandMark(modifier = Modifier.size(152.dp), pulse = markPulse)

            Spacer(Modifier.height(AstraSpacing.XL))

            Text(
                text = "ASTRA",
                style = AstraTypography.DisplayLarge.copy(letterSpacing = 10.sp),
                color = AstraColors.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(AstraSpacing.XS))
            Text(
                text = "LOCAL AI · PRIVATE BY DESIGN",
                style = AstraTypography.Caption.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                ),
                color = AstraColors.Secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier, pulse: Float) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        listOf(AstraColors.Secondary.copy(alpha = 0.22f * pulse), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .size(124.dp)
                .background(AstraColors.SurfaceElevated.copy(alpha = 0.82f), RoundedCornerShape(32.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                    ),
                    RoundedCornerShape(32.dp),
                )
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(88.dp).alpha(0.90f + 0.10f * pulse)) {
                drawAstraMonogram()
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAstraMonogram() {
    val primary = AstraColors.Primary
    val secondary = AstraColors.Secondary
    val gradient = Brush.linearGradient(
        colors = listOf(primary, secondary),
        start = Offset(size.width * 0.12f, size.height * 0.88f),
        end = Offset(size.width * 0.88f, size.height * 0.08f),
    )
    val glow = secondary.copy(alpha = 0.18f)
    val stroke = Stroke(
        width = size.minDimension * 0.17f,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )
    val aPath = Path().apply {
        moveTo(size.width * 0.18f, size.height * 0.82f)
        lineTo(size.width * 0.48f, size.height * 0.18f)
        quadraticTo(size.width * 0.52f, size.height * 0.09f, size.width * 0.58f, size.height * 0.18f)
        lineTo(size.width * 0.82f, size.height * 0.82f)
    }
    val sweepPath = Path().apply {
        moveTo(size.width * 0.12f, size.height * 0.72f)
        cubicTo(
            size.width * 0.34f,
            size.height * 0.53f,
            size.width * 0.62f,
            size.height * 0.45f,
            size.width * 0.90f,
            size.height * 0.48f,
        )
    }
    drawPath(aPath, color = glow, style = Stroke(width = size.minDimension * 0.25f, cap = StrokeCap.Round))
    drawPath(aPath, brush = gradient, style = stroke)
    drawPath(sweepPath, color = AstraColors.Background.copy(alpha = 0.65f), style = Stroke(width = size.minDimension * 0.20f, cap = StrokeCap.Round))
    drawPath(sweepPath, brush = gradient, style = Stroke(width = size.minDimension * 0.11f, cap = StrokeCap.Round))

    val starCenter = Offset(size.width * 0.78f, size.height * 0.28f)
    val star = Path().apply {
        moveTo(starCenter.x, starCenter.y - size.height * 0.13f)
        cubicTo(
            starCenter.x + size.width * 0.03f,
            starCenter.y - size.height * 0.03f,
            starCenter.x + size.width * 0.04f,
            starCenter.y - size.height * 0.02f,
            starCenter.x + size.width * 0.13f,
            starCenter.y,
        )
        cubicTo(
            starCenter.x + size.width * 0.04f,
            starCenter.y + size.height * 0.02f,
            starCenter.x + size.width * 0.03f,
            starCenter.y + size.height * 0.03f,
            starCenter.x,
            starCenter.y + size.height * 0.13f,
        )
        cubicTo(
            starCenter.x - size.width * 0.03f,
            starCenter.y + size.height * 0.03f,
            starCenter.x - size.width * 0.04f,
            starCenter.y + size.height * 0.02f,
            starCenter.x - size.width * 0.13f,
            starCenter.y,
        )
        cubicTo(
            starCenter.x - size.width * 0.04f,
            starCenter.y - size.height * 0.02f,
            starCenter.x - size.width * 0.03f,
            starCenter.y - size.height * 0.03f,
            starCenter.x,
            starCenter.y - size.height * 0.13f,
        )
        close()
    }
    drawPath(star, color = secondary.copy(alpha = 0.20f), style = Stroke(width = size.minDimension * 0.07f))
    drawPath(star, brush = Brush.radialGradient(listOf(Color.White, secondary), center = starCenter, radius = size.minDimension * 0.16f))
}

@Composable
private fun SplashBackdrop() {
    val infinite = rememberInfiniteTransition(label = "grid")
    val gridAlpha by infinite.animateFloat(
        initialValue = 0.025f,
        targetValue = 0.055f,
        animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Reverse),
        label = "grid-alpha",
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AstraColors.Primary.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(size.width * 0.50f, size.height * 0.38f),
                radius = size.minDimension * 0.62f,
            ),
            radius = size.minDimension * 0.62f,
            center = Offset(size.width * 0.50f, size.height * 0.38f),
        )
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
