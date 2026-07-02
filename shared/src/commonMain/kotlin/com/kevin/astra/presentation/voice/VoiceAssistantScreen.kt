package com.kevin.astra.presentation.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.design.AstraButton
import com.kevin.astra.core.design.AstraButtonStyle
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import com.kevin.astra.core.design.DemoModeBanner
import com.kevin.astra.core.design.MarkdownText
import com.kevin.astra.domain.settings.DemoModeHolder

private val BAR_MAX_HEIGHTS = listOf(0.55f, 0.80f, 0.65f, 1.00f, 0.70f, 0.90f, 0.60f, 0.85f, 0.50f)
private val BAR_DURATIONS   = listOf(420,   360,   480,   390,   450,   370,   500,   400,   460)

@Composable
fun VoiceAssistantScreen(
    viewModel: VoiceAssistantViewModel,
    contentPadding: PaddingValues,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isDemoMode by DemoModeHolder.enabled.collectAsStateWithLifecycle()
    val isListening = state.phase == VoicePhase.Listening

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = AstraSpacing.L)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AstraSpacing.M),
    ) {
        if (isDemoMode) DemoModeBanner(modifier = Modifier.padding(top = AstraSpacing.M))

        Spacer(Modifier.height(AstraSpacing.L))

        // Header
        Text(
            text = "ASTRA Voice",
            style = AstraTypography.Headline,
            color = AstraColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "On-device speech · No cloud · Private",
            style = AstraTypography.Caption,
            color = AstraColors.Secondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(AstraSpacing.L))

        // Phase indicator
        PhaseIndicator(phase = state.phase)

        Spacer(Modifier.height(AstraSpacing.M))

        // Waveform + mic button assembly
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            // Waveform bars behind the button
            WaveformVisualizer(
                isActive = isListening,
                modifier = Modifier
                    .width(260.dp)
                    .height(80.dp)
                    .align(Alignment.Center),
            )
            // Mic button centered on top
            MicButton(
                phase = state.phase,
                onClick = { viewModel.dispatch(VoiceAssistantIntent.ToggleMic) },
            )
        }

        Spacer(Modifier.height(AstraSpacing.S))

        // Idle hint — only show when nothing is happening yet
        AnimatedVisibility(
            visible = state.phase == VoicePhase.Idle && state.transcript.isBlank() && state.response.isBlank() && state.error == null,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(200)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AstraSpacing.M),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AstraSpacing.S),
            ) {
                Text(
                    text = "Tap the mic and speak your question",
                    style = AstraTypography.Body,
                    color = AstraColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Everything runs on-device — microphone access required",
                    style = AstraTypography.Caption,
                    color = AstraColors.TextDisabled,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Live transcript
        AnimatedVisibility(
            visible = state.transcript.isNotBlank(),
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200)),
        ) {
            BubbleCard(label = "You", text = state.transcript, color = AstraColors.Primary)
        }

        // AI response
        AnimatedVisibility(
            visible = state.response.isNotBlank(),
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200)),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                AiResponseCard(text = state.response)
                if (state.phase == VoicePhase.Speaking) {
                    AstraButton(
                        text = "⏹  Stop speaking",
                        onClick = { viewModel.dispatch(VoiceAssistantIntent.StopSpeaking) },
                        style = AstraButtonStyle.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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

        Spacer(Modifier.height(AstraSpacing.XL))
    }
}

// ── Phase indicator ───────────────────────────────────────────────────────────

@Composable
private fun PhaseIndicator(phase: VoicePhase) {
    val color = phaseColor(phase)
    Row(
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Text(
            text = phase.label,
            style = AstraTypography.Caption,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ── Waveform visualizer ───────────────────────────────────────────────────────

@Composable
private fun WaveformVisualizer(isActive: Boolean, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "waveform")
    val rawHeights = BAR_MAX_HEIGHTS.mapIndexed { i, maxH ->
        infinite.animateFloat(
            initialValue = 0.12f,
            targetValue = maxH,
            animationSpec = infiniteRepeatable(tween(BAR_DURATIONS[i]), RepeatMode.Reverse),
            label = "bar-$i",
        )
    }
    val factor by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(400),
        label = "waveform-factor",
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        rawHeights.forEachIndexed { i, rawH ->
            val height = 0.08f + (rawH.value - 0.08f) * factor
            val barColor = if (isActive) AstraColors.Secondary else AstraColors.TextDisabled
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(height)
                    .clip(RoundedCornerShape(3.dp))
                    .alpha(0.3f + 0.7f * factor)
                    .background(barColor),
            )
        }
    }
}

// ── Mic button ────────────────────────────────────────────────────────────────

@Composable
private fun MicButton(phase: VoicePhase, onClick: () -> Unit) {
    val isListening = phase == VoicePhase.Listening
    val isBusy = phase == VoicePhase.Processing
    val infinite = rememberInfiniteTransition(label = "mic-ring")
    val ringAlpha by infinite.animateFloat(
        initialValue = 0.3f, targetValue = 0.0f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "ring-alpha",
    )
    val bgColor = when (phase) {
        VoicePhase.Idle -> AstraColors.Primary
        VoicePhase.Listening -> AstraColors.Error
        VoicePhase.Processing -> AstraColors.Secondary
        VoicePhase.Speaking -> AstraColors.Secondary
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(bgColor.copy(alpha = ringAlpha), CircleShape)
                    .border(1.dp, bgColor.copy(alpha = ringAlpha + 0.1f), CircleShape),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .background(
                    if (isBusy) bgColor.copy(alpha = 0.5f) else bgColor.copy(alpha = 0.9f),
                    CircleShape,
                )
                .border(2.dp, bgColor, CircleShape)
                .clickable(enabled = !isBusy, onClick = onClick),
        ) {
            Text(
                text = when (phase) {
                    VoicePhase.Idle -> "🎤"
                    VoicePhase.Listening -> "⏹"
                    VoicePhase.Processing -> "⏳"
                    VoicePhase.Speaking -> "🔊"
                },
                style = AstraTypography.Title,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Cards ─────────────────────────────────────────────────────────────────────

@Composable
private fun BubbleCard(label: String, text: String, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(AstraSpacing.M),
    ) {
        Text(text = label, style = AstraTypography.Caption, color = color, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(AstraSpacing.XS))
        Text(text = text, style = AstraTypography.Body, color = AstraColors.TextPrimary)
    }
}

@Composable
private fun AiResponseCard(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AstraColors.SurfaceElevated, RoundedCornerShape(16.dp))
            .border(1.dp, AstraColors.Border, RoundedCornerShape(16.dp))
            .padding(AstraSpacing.M),
    ) {
        Text(
            text = "ASTRA",
            style = AstraTypography.Caption,
            color = AstraColors.Secondary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(AstraSpacing.XS))
        MarkdownText(text = text)
    }
}

@Composable
private fun ErrorCard(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AstraColors.Error.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .border(1.dp, AstraColors.Error.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(AstraSpacing.M),
    ) {
        Text(text = "⚠ Error", style = AstraTypography.Caption, color = AstraColors.Error, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(AstraSpacing.XS))
        Text(text = message, style = AstraTypography.Body, color = AstraColors.TextPrimary)
    }
}

private fun phaseColor(phase: VoicePhase): Color = when (phase) {
    VoicePhase.Idle -> AstraColors.TextDisabled
    VoicePhase.Listening -> AstraColors.Error
    VoicePhase.Processing -> AstraColors.Secondary
    VoicePhase.Speaking -> AstraColors.Primary
}
