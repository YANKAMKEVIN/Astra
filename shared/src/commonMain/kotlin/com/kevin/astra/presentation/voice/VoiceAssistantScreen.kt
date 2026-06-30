package com.kevin.astra.presentation.voice

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.design.AstraButton
import com.kevin.astra.core.design.AstraButtonStyle
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import com.kevin.astra.core.design.DemoModeBanner
import com.kevin.astra.domain.settings.DemoModeHolder

@Composable
fun VoiceAssistantScreen(
    viewModel: VoiceAssistantViewModel,
    contentPadding: PaddingValues,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isDemoMode by DemoModeHolder.enabled.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(AstraSpacing.L)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AstraSpacing.M),
    ) {
        if (isDemoMode) DemoModeBanner()

        Spacer(Modifier.height(AstraSpacing.L))

        // Header
        Text(
            text = "ASTRA Voice",
            style = AstraTypography.Headline,
            color = AstraColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "On-device voice assistant",
            style = AstraTypography.Body,
            color = AstraColors.TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(AstraSpacing.L))

        // Phase label
        Text(
            text = state.phase.label,
            style = AstraTypography.Title,
            color = phaseColor(state.phase),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(AstraSpacing.M))

        // Mic button
        MicButton(
            phase = state.phase,
            onClick = { viewModel.dispatch(VoiceAssistantIntent.ToggleMic) },
        )

        Spacer(Modifier.height(AstraSpacing.M))

        // Transcript (live STT)
        if (state.transcript.isNotBlank()) {
            BubbleCard(
                label = "You said",
                text = state.transcript,
                color = AstraColors.Primary,
            )
        }

        // Response
        if (state.response.isNotBlank()) {
            BubbleCard(
                label = "ASTRA",
                text = state.response,
                color = AstraColors.Secondary,
            )
            if (state.phase == VoicePhase.Speaking) {
                AstraButton(
                    text = "⏹ Stop",
                    onClick = { viewModel.dispatch(VoiceAssistantIntent.StopSpeaking) },
                    style = AstraButtonStyle.Secondary,
                )
            }
        }

        // Error
        if (state.error != null) {
            ErrorCard(message = state.error!!)
            AstraButton(
                text = "Dismiss",
                onClick = { viewModel.dispatch(VoiceAssistantIntent.ClearError) },
                style = AstraButtonStyle.Ghost,
            )
        }

        Spacer(Modifier.height(AstraSpacing.L))
    }
}

@Composable
private fun MicButton(phase: VoicePhase, onClick: () -> Unit) {
    val isListening = phase == VoicePhase.Listening
    val isBusy = phase == VoicePhase.Processing

    val infiniteTransition = rememberInfiniteTransition(label = "mic-pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.18f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse-scale",
    )

    val bgColor = when (phase) {
        VoicePhase.Idle -> AstraColors.Primary
        VoicePhase.Listening -> AstraColors.Error
        VoicePhase.Processing -> AstraColors.Secondary
        VoicePhase.Speaking -> AstraColors.Secondary
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(120.dp)
            .scale(if (isListening) pulseScale else 1f),
    ) {
        // Outer glow ring (listening only)
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(bgColor.copy(alpha = 0.25f)),
            )
        }
        // Main button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(if (isBusy) bgColor.copy(alpha = 0.5f) else bgColor)
                .clickable(enabled = !isBusy, onClick = onClick),
        ) {
            Text(
                text = when (phase) {
                    VoicePhase.Idle -> "🎤"
                    VoicePhase.Listening -> "⏹"
                    VoicePhase.Processing -> "⏳"
                    VoicePhase.Speaking -> "⏹"
                },
                fontSize = 32.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BubbleCard(label: String, text: String, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(AstraSpacing.M),
    ) {
        Text(
            text = label,
            style = AstraTypography.Caption,
            color = color,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(AstraSpacing.XS))
        Text(
            text = text,
            style = AstraTypography.Body,
            color = AstraColors.TextPrimary,
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AstraColors.Error.copy(alpha = 0.12f))
            .padding(AstraSpacing.M),
    ) {
        Text(
            text = "⚠ Error",
            style = AstraTypography.Caption,
            color = AstraColors.Error,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(AstraSpacing.XS))
        Text(
            text = message,
            style = AstraTypography.Body,
            color = AstraColors.TextPrimary,
        )
    }
}

@Composable
private fun phaseColor(phase: VoicePhase): Color = when (phase) {
    VoicePhase.Idle -> AstraColors.TextSecondary
    VoicePhase.Listening -> AstraColors.Error
    VoicePhase.Processing -> AstraColors.Secondary
    VoicePhase.Speaking -> AstraColors.Primary
}
