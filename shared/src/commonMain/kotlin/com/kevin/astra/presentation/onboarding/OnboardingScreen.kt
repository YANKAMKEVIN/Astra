package com.kevin.astra.presentation.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import com.kevin.astra.core.design.AstraButton
import com.kevin.astra.core.design.AstraButtonStyle
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography

private data class OnboardingSlide(
    val visual: OnboardingVisual,
    val title: String,
    val description: String,
    val badge: String,
)

private enum class OnboardingVisual {
    Private,
    Workspace,
    Hardware,
}

private val slides = listOf(
    OnboardingSlide(
        visual = OnboardingVisual.Private,
        title = "Private by design",
        description = "Run capable AI models locally. Your prompts, files, and conversations stay on this device by default.",
        badge = "LOCAL FIRST",
    ),
    OnboardingSlide(
        visual = OnboardingVisual.Workspace,
        title = "One workspace for AI",
        description = "Chat, voice, vision, and documents live in the same focused cockpit, ready when you need them.",
        badge = "MULTIMODAL",
    ),
    OnboardingSlide(
        visual = OnboardingVisual.Hardware,
        title = "Tuned to your hardware",
        description = "Choose models, measure performance, and keep ASTRA responsive on the device in your hands.",
        badge = "EDGE READY",
    ),
)

@Composable
fun OnboardingScreen(
    contentPadding: PaddingValues,
    onFinished: () -> Unit,
) {
    var currentSlide by remember { mutableIntStateOf(0) }
    var lastSlide by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AstraColors.Background)
            .padding(contentPadding),
    ) {
        // Neural dot grid backdrop
        OnboardingDotGrid()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AstraSpacing.L, vertical = AstraSpacing.XL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(AstraColors.Secondary, CircleShape),
                )
                Text(
                    text = "ASTRA",
                    style = AstraTypography.Caption.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 3.sp,
                    ),
                    color = AstraColors.Secondary,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Animated slide content
            AnimatedContent(
                targetState = currentSlide,
                transitionSpec = {
                    val forward = targetState > initialState
                    if (forward) {
                        (slideInHorizontally(tween(320)) { it / 2 } + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(240)) { -it / 2 } + fadeOut(tween(240)))
                    } else {
                        (slideInHorizontally(tween(320)) { -it / 2 } + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(240)) { it / 2 } + fadeOut(tween(240)))
                    }
                },
                label = "slide",
            ) { slide ->
                SlideContent(slide = slides[slide])
            }

            // Bottom controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AstraSpacing.M),
            ) {
                SegmentedProgress(total = slides.size, current = currentSlide)

                if (currentSlide < slides.size - 1) {
                    AstraButton(
                        text = "Continue",
                        onClick = { lastSlide = currentSlide; currentSlide++ },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AstraButton(
                        text = "Skip",
                        onClick = onFinished,
                        style = AstraButtonStyle.Ghost,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    AstraButton(
                        text = "Start locally",
                        onClick = onFinished,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// ── Slide content ─────────────────────────────────────────────────────────────

@Composable
private fun SlideContent(slide: OnboardingSlide) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AstraSpacing.L),
    ) {
        // Premium mark tile
        Box(
            modifier = Modifier.size(132.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            listOf(AstraColors.Secondary.copy(alpha = 0.22f), Color.Transparent),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .background(AstraColors.SurfaceElevated.copy(alpha = 0.72f), RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                        ),
                        RoundedCornerShape(28.dp),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                OnboardingVisualMark(visual = slide.visual)
            }
        }

        // Text content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AstraSpacing.M),
        ) {
            // Badge
            Box(
                modifier = Modifier
                    .background(AstraColors.Secondary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .border(1.dp, AstraColors.Secondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = AstraSpacing.S, vertical = AstraSpacing.XS),
            ) {
                Text(
                    text = slide.badge,
                    style = AstraTypography.Caption.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                    ),
                    color = AstraColors.Secondary,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = slide.title,
                style = AstraTypography.Headline,
                color = AstraColors.TextPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = slide.description,
                style = AstraTypography.Body,
                color = AstraColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun OnboardingVisualMark(visual: OnboardingVisual) {
    Canvas(modifier = Modifier.size(72.dp)) {
        val accent = AstraColors.Secondary
        val primary = AstraColors.Primary
        val muted = AstraColors.Border.copy(alpha = 0.70f)
        val stroke = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        val glowStroke = Stroke(
            width = 8.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )

        when (visual) {
            OnboardingVisual.Private -> {
                val shield = Path().apply {
                    moveTo(size.width * 0.50f, size.height * 0.08f)
                    cubicTo(
                        size.width * 0.66f,
                        size.height * 0.17f,
                        size.width * 0.78f,
                        size.height * 0.20f,
                        size.width * 0.86f,
                        size.height * 0.22f,
                    )
                    lineTo(size.width * 0.80f, size.height * 0.55f)
                    cubicTo(
                        size.width * 0.76f,
                        size.height * 0.76f,
                        size.width * 0.62f,
                        size.height * 0.88f,
                        size.width * 0.50f,
                        size.height * 0.94f,
                    )
                    cubicTo(
                        size.width * 0.38f,
                        size.height * 0.88f,
                        size.width * 0.24f,
                        size.height * 0.76f,
                        size.width * 0.20f,
                        size.height * 0.55f,
                    )
                    lineTo(size.width * 0.14f, size.height * 0.22f)
                    cubicTo(
                        size.width * 0.22f,
                        size.height * 0.20f,
                        size.width * 0.34f,
                        size.height * 0.17f,
                        size.width * 0.50f,
                        size.height * 0.08f,
                    )
                    close()
                }
                drawPath(shield, color = primary.copy(alpha = 0.16f))
                drawPath(shield, color = accent.copy(alpha = 0.22f), style = glowStroke)
                drawPath(shield, brush = Brush.linearGradient(listOf(primary, accent)), style = stroke)
                drawLine(
                    color = Color.White.copy(alpha = 0.90f),
                    start = Offset(size.width * 0.34f, size.height * 0.50f),
                    end = Offset(size.width * 0.46f, size.height * 0.62f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.90f),
                    start = Offset(size.width * 0.46f, size.height * 0.62f),
                    end = Offset(size.width * 0.68f, size.height * 0.38f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            OnboardingVisual.Workspace -> {
                repeat(3) { index ->
                    val offset = index * 8.dp.toPx()
                    val topLeft = Offset(size.width * 0.16f + offset, size.height * 0.22f + offset)
                    val rectSize = androidx.compose.ui.geometry.Size(size.width * 0.54f, size.height * 0.40f)
                    drawRoundRect(
                        color = if (index == 2) primary.copy(alpha = 0.18f) else Color.Transparent,
                        topLeft = topLeft,
                        size = rectSize,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
                    )
                    drawRoundRect(
                        color = if (index == 2) accent else muted,
                        topLeft = topLeft,
                        size = rectSize,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
                drawCircle(
                    color = accent,
                    radius = 3.5.dp.toPx(),
                    center = Offset(size.width * 0.37f, size.height * 0.80f),
                )
                drawCircle(
                    color = primary,
                    radius = 3.5.dp.toPx(),
                    center = Offset(size.width * 0.50f, size.height * 0.80f),
                )
                drawCircle(
                    color = accent.copy(alpha = 0.55f),
                    radius = 3.5.dp.toPx(),
                    center = Offset(size.width * 0.63f, size.height * 0.80f),
                )
            }

            OnboardingVisual.Hardware -> {
                val chipLeft = size.width * 0.22f
                val chipTop = size.height * 0.22f
                val chipSize = size.width * 0.56f
                drawRoundRect(
                    color = primary.copy(alpha = 0.15f),
                    topLeft = Offset(chipLeft, chipTop),
                    size = androidx.compose.ui.geometry.Size(chipSize, chipSize),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                )
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(primary, accent)),
                    topLeft = Offset(chipLeft, chipTop),
                    size = androidx.compose.ui.geometry.Size(chipSize, chipSize),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                    style = stroke,
                )
                val pins = listOf(0.32f, 0.50f, 0.68f)
                pins.forEach { fraction ->
                    drawLine(
                        color = muted,
                        start = Offset(size.width * fraction, size.height * 0.10f),
                        end = Offset(size.width * fraction, chipTop),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = muted,
                        start = Offset(size.width * fraction, chipTop + chipSize),
                        end = Offset(size.width * fraction, size.height * 0.90f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                val pulse = Path().apply {
                    moveTo(size.width * 0.32f, size.height * 0.54f)
                    lineTo(size.width * 0.42f, size.height * 0.54f)
                    lineTo(size.width * 0.47f, size.height * 0.42f)
                    lineTo(size.width * 0.55f, size.height * 0.66f)
                    lineTo(size.width * 0.62f, size.height * 0.50f)
                    lineTo(size.width * 0.70f, size.height * 0.50f)
                }
                drawPath(pulse, color = Color.White.copy(alpha = 0.92f), style = stroke)
            }
        }
    }
}

// ── Segmented progress bar ────────────────────────────────────────────────────

@Composable
private fun SegmentedProgress(total: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            val filled by animateFloatAsState(
                targetValue = if (index <= current) 1f else 0f,
                animationSpec = tween(300),
                label = "seg-$index",
            )
            Box(
                modifier = Modifier
                    .height(3.dp)
                    .width(if (index == current) 28.dp else 18.dp)
                    .background(AstraColors.SurfaceElevated, RoundedCornerShape(2.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(filled)
                        .height(3.dp)
                        .background(
                            if (index < current) AstraColors.Primary else AstraColors.Secondary,
                            RoundedCornerShape(2.dp),
                        ),
                )
            }
        }
    }
}

// ── Dot grid backdrop ─────────────────────────────────────────────────────────

@Composable
private fun OnboardingDotGrid() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cols = 10
        val rows = 18
        val spacingX = size.width / cols
        val spacingY = size.height / rows
        for (col in 0..cols) {
            for (row in 0..rows) {
                drawCircle(
                    color = AstraColors.Primary.copy(alpha = 0.04f),
                    radius = 1.5.dp.toPx(),
                    center = Offset(col * spacingX, row * spacingY),
                )
            }
        }
    }
}
