package com.kevin.astra.presentation.vision

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.design.AstraButton
import com.kevin.astra.core.design.AstraButtonStyle
import com.kevin.astra.core.design.AstraCard
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraScreen
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import com.kevin.astra.core.design.DemoModeBanner
import com.kevin.astra.domain.settings.DemoModeHolder
import com.kevin.astra.domain.vision.ImageClassificationResult

@Composable
fun VisionAssistantScreen(
    viewModel: VisionAssistantViewModel,
    contentPadding: PaddingValues,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isDemoMode by DemoModeHolder.enabled.collectAsStateWithLifecycle()
    val captureLauncher = rememberImageCaptureLauncher { bytes ->
        viewModel.dispatch(VisionAssistantIntent.PhotoCaptured(bytes))
    }

    AstraScreen(
        title = "Vision",
        description = "Point the camera at anything. ASTRA will describe and analyze it.",
        contentPadding = contentPadding,
        showDemoIndicator = false,
    ) {
        if (isDemoMode) DemoModeBanner()
        // Mock classifier warning
        if (!state.classifierAvailable) {
            MockWarningBanner()
        }

        // Question input
        OutlinedTextField(
            value = state.userQuestion,
            onValueChange = { viewModel.dispatch(VisionAssistantIntent.UpdateQuestion(it)) },
            label = { Text("Your question", color = AstraColors.TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AstraColors.Primary,
                unfocusedBorderColor = AstraColors.Border,
                cursorColor = AstraColors.Primary,
                focusedTextColor = AstraColors.TextPrimary,
                unfocusedTextColor = AstraColors.TextPrimary,
            ),
        )

        // Camera button
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraButton(
                text = "📷  Take Photo",
                onClick = captureLauncher,
                modifier = Modifier.weight(1f),
                enabled = state.phase == VisionPhase.Idle || state.phase == VisionPhase.Done,
            )
            if (state.phase == VisionPhase.Done || state.capturedImageBytes != null) {
                AstraButton(
                    text = "Reset",
                    onClick = { viewModel.dispatch(VisionAssistantIntent.Reset) },
                    style = AstraButtonStyle.Secondary,
                )
            }
        }

        // Progress
        when (state.phase) {
            VisionPhase.Classifying -> StatusRow("Analyzing image with Vision model…", AstraColors.Secondary)
            VisionPhase.Thinking -> StatusRow("Asking ASTRA…", AstraColors.Primary)
            else -> {}
        }

        // Captured image preview
        state.capturedImageBytes?.let { bytes ->
            decodeImageBytes(bytes)?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = "Captured image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        // Classification result
        state.classificationResult?.let { result ->
            ClassificationCard(result)
        }

        // ASTRA response
        if (state.response.isNotBlank()) {
            AstraCard(title = "ASTRA Analysis") {
                Text(
                    text = state.response,
                    style = AstraTypography.Body,
                    color = AstraColors.TextPrimary,
                )
            }
            AstraButton(
                text = "Export",
                onClick = { viewModel.dispatch(VisionAssistantIntent.ExportResponse) },
                style = AstraButtonStyle.Ghost,
            )
        }

        // Error
        if (state.error != null) {
            ErrorBanner(message = state.error!!)
            AstraButton(
                text = "Dismiss",
                onClick = { viewModel.dispatch(VisionAssistantIntent.ClearError) },
                style = AstraButtonStyle.Ghost,
            )
        }
    }
}

@Composable
private fun MockWarningBanner() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AstraColors.Secondary.copy(alpha = 0.12f))
            .padding(AstraSpacing.M),
    ) {
        Text(
            text = "⚙ Mock vision mode",
            style = AstraTypography.Caption,
            color = AstraColors.Secondary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(AstraSpacing.XS))
        Text(
            text = "Add EfficientNet-Lite0 (model.tflite + labels.txt) to assets/models/vision/ for real on-device inference.",
            style = AstraTypography.Caption,
            color = AstraColors.TextSecondary,
        )
    }
}

@Composable
private fun StatusRow(message: String, color: androidx.compose.ui.graphics.Color) {
    Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.XS)) {
        Text(text = message, style = AstraTypography.Caption, color = color)
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = AstraColors.Border,
        )
    }
}

@Composable
private fun ClassificationCard(result: ImageClassificationResult) {
    AstraCard(title = "Detected") {
        result.labels.forEach { label ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label.label,
                    style = AstraTypography.Body,
                    color = AstraColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${(label.confidence * 100).toInt()}%",
                    style = AstraTypography.Caption,
                    color = AstraColors.Primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                )
            }
            Spacer(Modifier.height(AstraSpacing.XS))
        }
        if (!result.modelUsed.contains("Mock", ignoreCase = false)) {
            Spacer(Modifier.height(AstraSpacing.XS))
            Text(
                text = "via ${result.modelUsed}",
                style = AstraTypography.Caption,
                color = AstraColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AstraColors.Error.copy(alpha = 0.12f))
            .padding(AstraSpacing.M),
    ) {
        Text("⚠ Error", style = AstraTypography.Caption, color = AstraColors.Error, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(AstraSpacing.XS))
        Text(message, style = AstraTypography.Body, color = AstraColors.TextPrimary)
    }
}
