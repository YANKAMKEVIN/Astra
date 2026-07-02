package com.kevin.astra.presentation.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.design.AstraButton
import com.kevin.astra.core.design.AstraButtonStyle
import com.kevin.astra.core.design.AstraCard
import com.kevin.astra.core.design.AstraChip
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraMetricCard
import com.kevin.astra.core.design.AstraScreen
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography

@Composable
fun DemoScreen(
    contentPadding: PaddingValues,
    viewModel: DemoViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DemoContent(
        state = state,
        contentPadding = contentPadding,
        onIntent = viewModel::dispatch,
    )
}

@Composable
private fun DemoContent(
    state: DemoState,
    contentPadding: PaddingValues,
    onIntent: (DemoIntent) -> Unit,
) {
    AstraScreen(
        title = "Demo Mode",
        description = "A guided five-minute path for engineers, managers and innovation committees.",
        contentPadding = contentPadding,
    ) {
        DemoOverviewCard(state = state, onRefresh = { onIntent(DemoIntent.Refresh) })
        ReadinessCard(indicators = state.readinessIndicators)
        GuidedDemoCard(state = state, onIntent = onIntent)
        CurrentStepCard(state = state)
    }
}

@Composable
private fun DemoOverviewCard(
    state: DemoState,
    onRefresh: () -> Unit,
) {
    AstraCard(
        title = "Live demonstration overview",
        subtitle = "One-glance readiness for a professional ASTRA walkthrough.",
        status = if (state.isDemoReady) "DEMO READY" else "CHECK SETUP",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraMetricCard(
                value = state.capabilities?.platform ?: "Scanning",
                unit = "",
                label = "Device status",
                modifier = Modifier.weight(1f),
            )
            AstraMetricCard(
                value = state.selectedBackend?.displayName ?: "None",
                unit = "",
                label = "Selected runtime",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(AstraSpacing.S))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraMetricCard(
                value = state.selectedModel?.displayName ?: "None",
                unit = "",
                label = "Selected model",
                modifier = Modifier.weight(1f),
            )
            AstraMetricCard(
                value = state.selectedIndustry?.label ?: "None",
                unit = "",
                label = "Industry",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(AstraSpacing.S))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraMetricCard(
                value = state.selectedBackend?.status?.label ?: "Unknown",
                unit = "",
                label = "Backend status",
                modifier = Modifier.weight(1f),
            )
            AstraMetricCard(
                value = "${state.progressPercent}",
                unit = "%",
                label = "Demo progress",
                modifier = Modifier.weight(1f),
            )
        }
        if (state.error != null) {
            Spacer(Modifier.height(AstraSpacing.S))
            Text(
                text = state.error,
                style = AstraTypography.Caption,
                color = AstraColors.Error,
            )
        }
        Spacer(Modifier.height(AstraSpacing.M))
        AstraButton(
            text = if (state.isLoadingCapabilities) "Refreshing..." else "Refresh readiness",
            onClick = onRefresh,
            style = AstraButtonStyle.Ghost,
            enabled = !state.isLoadingCapabilities,
        )
    }
}

@Composable
private fun ReadinessCard(indicators: List<DemoReadinessIndicator>) {
    AstraCard(
        title = "Demo readiness",
        subtitle = "The indicators below explain whether ASTRA is safe to demo live.",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            indicators.forEach { indicator ->
                ReadinessRow(indicator = indicator)
            }
        }
    }
}

@Composable
private fun ReadinessRow(indicator: DemoReadinessIndicator) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AstraColors.SurfaceElevated, RoundedCornerShape(16.dp))
            .border(1.dp, AstraColors.Border, RoundedCornerShape(16.dp))
            .padding(AstraSpacing.M),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = indicator.label,
                style = AstraTypography.Body,
                color = AstraColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(AstraSpacing.XS))
            Text(
                text = indicator.message,
                style = AstraTypography.Caption,
                color = AstraColors.TextSecondary,
            )
        }
        AstraChip(
            label = indicator.status.label.uppercase(),
            color = indicator.status.color(),
        )
    }
}

@Composable
private fun GuidedDemoCard(
    state: DemoState,
    onIntent: (DemoIntent) -> Unit,
) {
    AstraCard(
        title = "Guided demo workflow",
        subtitle = "Follow the flow from hardware readiness through model transparency.",
        status = "${DemoStep.entries.indexOf(state.currentStep) + 1}/${DemoStep.entries.size}",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        ) {
            DemoStep.entries.forEachIndexed { index, step ->
                DemoStepPill(
                    number = index + 1,
                    step = step,
                    selected = state.currentStep == step,
                    completed = step in state.completedSteps,
                    onClick = { onIntent(DemoIntent.SelectStep(step)) },
                )
            }
        }
        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraButton(
                text = "Previous",
                onClick = { onIntent(DemoIntent.PreviousStep) },
                style = AstraButtonStyle.Ghost,
                modifier = Modifier.weight(1f),
                enabled = state.currentStep != DemoStep.DeviceCapabilities,
            )
            AstraButton(
                text = if (state.currentStep == DemoStep.ModelManager) "Complete flow" else "Next step",
                onClick = { onIntent(DemoIntent.NextStep) },
                style = AstraButtonStyle.Primary,
                modifier = Modifier.weight(1f),
                enabled = state.currentStep != DemoStep.ModelManager,
            )
        }
    }
}

@Composable
private fun DemoStepPill(
    number: Int,
    step: DemoStep,
    selected: Boolean,
    completed: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .heightIn(min = 96.dp)
            .background(
                color = when {
                    selected -> AstraColors.Primary.copy(alpha = 0.18f)
                    completed -> AstraColors.Success.copy(alpha = 0.12f)
                    else -> AstraColors.SurfaceElevated
                },
                shape = RoundedCornerShape(18.dp),
            )
            .border(
                width = 1.dp,
                color = when {
                    selected -> AstraColors.Secondary
                    completed -> AstraColors.Success
                    else -> AstraColors.Border
                },
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(AstraSpacing.M),
        verticalArrangement = Arrangement.Center,
    ) {
        AstraChip(
            label = if (completed) "DONE" else "STEP $number",
            color = if (completed) AstraColors.Success else AstraColors.Secondary,
        )
        Spacer(Modifier.height(AstraSpacing.S))
        Text(
            text = step.label,
            style = AstraTypography.Body,
            color = AstraColors.TextPrimary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun CurrentStepCard(state: DemoState) {
    AstraCard(
        title = state.currentStep.label,
        subtitle = state.currentStep.description,
        status = "CURRENT",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Text(
            text = state.currentStep.guidance(state),
            style = AstraTypography.Body,
            color = AstraColors.TextPrimary,
        )
    }
}

private fun DemoStep.guidance(state: DemoState): String =
    when (this) {
        DemoStep.DeviceCapabilities ->
            "Open with the device: ${state.capabilities?.deviceModel ?: "scanning"} on ${state.capabilities?.platform ?: "unknown platform"}. Confirm offline mode and local execution."
        DemoStep.RuntimeSelection ->
            "Explain the selected runtime: ${state.selectedBackend?.displayName ?: "none"}. Status: ${state.selectedBackend?.status?.label ?: "unknown"}."
        DemoStep.Assistant ->
            "Move to Assistant and run one operational question using the ${state.selectedIndustry?.label ?: "General"} persona."
        DemoStep.DocumentsAssistant ->
            "Move to Documents and show how embedded maintenance context grounds the answer without external services."
        DemoStep.Benchmark ->
            "Move to Benchmark and run the scenario comparison to discuss latency, runtime mode and quality."
        DemoStep.TaskEvaluation ->
            "Use the Benchmark result's Task Evaluation breakdown to explain safety, completeness, technical accuracy, terminology and clarity."
        DemoStep.ModelManager ->
            "Close in Settings with Model Manager: show local file readiness and why Mock fallback remains active when production bundles are missing."
    }

private fun DemoReadinessStatus.color(): Color =
    when (this) {
        DemoReadinessStatus.Ready -> AstraColors.Success
        DemoReadinessStatus.Warning -> AstraColors.Warning
        DemoReadinessStatus.Blocked -> AstraColors.Error
    }
