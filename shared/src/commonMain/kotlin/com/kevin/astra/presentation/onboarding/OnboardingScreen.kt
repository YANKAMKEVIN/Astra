package com.kevin.astra.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kevin.astra.core.design.AstraButton
import com.kevin.astra.core.design.AstraButtonStyle
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography

private data class OnboardingSlide(
    val icon: String,
    val title: String,
    val description: String,
)

private val slides = listOf(
    OnboardingSlide(
        icon = "⚡",
        title = "100% On-Device AI",
        description = "ASTRA runs Small Language Models directly on your device. No cloud. No data sent. Full privacy — even offline.",
    ),
    OnboardingSlide(
        icon = "🎙️",
        title = "Voice · Vision · RAG",
        description = "Speak to ASTRA, analyze images with on-device EfficientNet, and query local PDFs with TF-IDF retrieval — all in real time.",
    ),
    OnboardingSlide(
        icon = "📊",
        title = "Benchmark Your Hardware",
        description = "Measure tokens/s, battery drain, and temperature across models. Find the best SLM for your device and use case.",
    ),
)

@Composable
fun OnboardingScreen(
    contentPadding: PaddingValues,
    onFinished: () -> Unit,
) {
    var currentSlide by remember { mutableStateOf(0) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AstraColors.Background)
            .padding(contentPadding),
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(400)) + slideInHorizontally(tween(400)) { it / 4 },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AstraSpacing.L, vertical = AstraSpacing.XL),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // App label
                Text(
                    text = "ASTRA",
                    style = AstraTypography.Caption,
                    color = AstraColors.Secondary,
                )

                // Slide content
                SlideContent(slide = slides[currentSlide])

                // Bottom controls
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Dots
                    PagerDots(count = slides.size, current = currentSlide)

                    Spacer(Modifier.height(AstraSpacing.L))

                    // Buttons
                    if (currentSlide < slides.size - 1) {
                        AstraButton(
                            text = "Next",
                            onClick = { currentSlide++ },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(AstraSpacing.S))
                        AstraButton(
                            text = "Skip",
                            onClick = onFinished,
                            style = AstraButtonStyle.Ghost,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        AstraButton(
                            text = "Get Started",
                            onClick = onFinished,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(Modifier.height(AstraSpacing.M))
                }
            }
        }
    }
}

@Composable
private fun SlideContent(slide: OnboardingSlide) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = slide.icon,
            style = AstraTypography.Title.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified),
            modifier = Modifier.padding(bottom = AstraSpacing.M),
        )
        Text(
            text = slide.title,
            style = AstraTypography.Title,
            color = AstraColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AstraSpacing.M))
        Text(
            text = slide.description,
            style = AstraTypography.Body,
            color = AstraColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PagerDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            val alpha by animateFloatAsState(
                targetValue = if (index == current) 1f else 0.3f,
                label = "dot-alpha",
            )
            Box(
                modifier = Modifier
                    .size(if (index == current) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(AstraColors.Secondary)
                    .alpha(alpha),
            )
        }
    }
}
