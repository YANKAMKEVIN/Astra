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
    val icon: String,
    val title: String,
    val description: String,
    val badge: String,
)

private val slides = listOf(
    OnboardingSlide(
        icon = "⚡",
        title = "100% On-Device AI",
        description = "ASTRA runs Small Language Models directly on your device. No cloud. No data sent. Full privacy — even in air-gapped environments.",
        badge = "ZERO-CLOUD",
    ),
    OnboardingSlide(
        icon = "🎙",
        title = "Voice · Vision · RAG",
        description = "Speak to ASTRA, analyze images with on-device EfficientNet, and query local PDFs with TF-IDF retrieval — all processed in real time.",
        badge = "MULTIMODAL",
    ),
    OnboardingSlide(
        icon = "📊",
        title = "Benchmark Your Hardware",
        description = "Measure tokens/s, memory pressure, and model load times. Find the optimal SLM configuration for your specific device and use case.",
        badge = "EDGE METRICS",
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
                        text = "Get Started →",
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
        // Icon with orbital ring
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Outer ring
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(AstraColors.Primary.copy(alpha = 0.06f), CircleShape)
                    .border(1.dp, AstraColors.Primary.copy(alpha = 0.15f), CircleShape),
            )
            // Inner ring
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(AstraColors.SurfaceElevated, CircleShape)
                    .border(1.dp, AstraColors.Border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = slide.icon, style = AstraTypography.Headline)
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
