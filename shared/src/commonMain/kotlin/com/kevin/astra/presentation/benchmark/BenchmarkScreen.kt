package com.kevin.astra.presentation.benchmark

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.design.AstraButton
import com.kevin.astra.core.design.AstraCard
import com.kevin.astra.core.design.AstraChip
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraMetricCard
import com.kevin.astra.core.design.AstraScreen
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import com.kevin.astra.domain.benchmark.BenchmarkResult

@Composable
fun BenchmarkScreen(
    contentPadding: PaddingValues,
    viewModel: BenchmarkViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BenchmarkContent(
        state = state,
        contentPadding = contentPadding,
        onIntent = viewModel::dispatch,
    )
}

@Composable
private fun BenchmarkContent(
    state: BenchmarkState,
    contentPadding: PaddingValues,
    onIntent: (BenchmarkIntent) -> Unit,
) {
    AstraScreen(
        title = "Benchmark Lab",
        description = "Compare simulated on-device AI models with measurable Edge AI telemetry.",
        contentPadding = contentPadding,
    ) {
        BenchmarkPromptCard(
            prompt = state.prompt,
            isRunning = state.isRunning,
            onPromptChanged = { onIntent(BenchmarkIntent.UpdatePrompt(it)) },
        )
        ModelSelectionCard(
            selectedModels = state.selectedModels,
            isRunning = state.isRunning,
            onToggleModel = { onIntent(BenchmarkIntent.ToggleModel(it)) },
        )
        BackendSelectionCard(
            selectedBackend = state.selectedBackend,
            isRunning = state.isRunning,
            onSelectBackend = { onIntent(BenchmarkIntent.SelectBackend(it)) },
        )
        RunBenchmarkCard(state = state, onRun = { onIntent(BenchmarkIntent.RunBenchmark) })

        AnimatedVisibility(visible = state.isRunning) {
            AstraCard(
                title = "Running benchmark...",
                subtitle = "ASTRA is simulating model execution using deterministic local profiles.",
                status = "RUNNING",
            ) {
                Spacer(Modifier.height(AstraSpacing.M))
                AstraChip(label = "Mock execution", color = AstraColors.Secondary)
            }
        }

        state.recommendedModel?.let { recommendation ->
            AstraCard(
                title = "Recommended model",
                subtitle = recommendation.explanation,
                status = recommendation.model.label,
            ) {
                Spacer(Modifier.height(AstraSpacing.M))
                AstraChip(label = "BEST SIMULATED FIT", color = AstraColors.Success)
            }
        }

        ResultsCard(
            results = state.results,
            recommendedModel = state.recommendedModel?.model,
        )
    }
}

@Composable
private fun BenchmarkPromptCard(
    prompt: String,
    isRunning: Boolean,
    onPromptChanged: (String) -> Unit,
) {
    AstraCard(
        title = "Benchmark Prompt",
        subtitle = "Every selected model is evaluated against the same operational prompt.",
        status = "EDITABLE",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        BasicTextField(
            value = prompt,
            onValueChange = onPromptChanged,
            enabled = !isRunning,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .alpha(if (isRunning) 0.64f else 1f),
            textStyle = AstraTypography.Body.copy(color = AstraColors.TextPrimary),
            cursorBrush = SolidColor(AstraColors.Secondary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .background(AstraColors.SurfaceElevated, RoundedCornerShape(20.dp))
                        .border(1.dp, AstraColors.Border, RoundedCornerShape(20.dp))
                        .padding(AstraSpacing.M),
                ) {
                    if (prompt.isBlank()) {
                        Text(
                            text = DefaultBenchmarkPrompt,
                            style = AstraTypography.Body,
                            color = AstraColors.TextDisabled,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun ModelSelectionCard(
    selectedModels: Set<AiModel>,
    isRunning: Boolean,
    onToggleModel: (AiModel) -> Unit,
) {
    AstraCard(
        title = "Model Selection",
        subtitle = "All model profiles are selectable in this mocked lab.",
        status = "${selectedModels.size} SELECTED",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        HorizontalOptions {
            AiModel.entries.forEach { model ->
                SelectablePill(
                    label = model.label,
                    status = if (model in selectedModels) "SELECTED" else "AVAILABLE",
                    selected = model in selectedModels,
                    enabled = !isRunning,
                    onClick = { onToggleModel(model) },
                )
            }
        }
    }
}

@Composable
private fun BackendSelectionCard(
    selectedBackend: InferenceBackend,
    isRunning: Boolean,
    onSelectBackend: (InferenceBackend) -> Unit,
) {
    AstraCard(
        title = "Backend Selection",
        subtitle = "Mock Engine executes this benchmark. Future backends are visible but unavailable.",
        status = selectedBackend.label,
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        HorizontalOptions {
            InferenceBackend.entries.forEach { backend ->
                SelectablePill(
                    label = backend.label,
                    status = if (backend == InferenceBackend.Mock) "ACTIVE" else "COMING SOON",
                    selected = backend == selectedBackend,
                    enabled = backend == InferenceBackend.Mock && !isRunning,
                    onClick = { onSelectBackend(backend) },
                )
            }
        }
    }
}

@Composable
private fun RunBenchmarkCard(
    state: BenchmarkState,
    onRun: () -> Unit,
) {
    AstraCard(
        title = "Execution",
        subtitle = "Run a deterministic mock benchmark and compute the best model recommendation.",
        status = if (state.isRunning) "RUNNING" else "READY",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraMetricCard(
                value = state.selectedModels.size.toString(),
                unit = "",
                label = "Models",
                modifier = Modifier.weight(1f),
            )
            AstraMetricCard(
                value = state.results.size.toString(),
                unit = "",
                label = "Results",
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
            text = "Run Benchmark",
            onClick = onRun,
            enabled = state.canRun,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ResultsCard(
    results: List<BenchmarkResult>,
    recommendedModel: AiModel?,
) {
    AstraCard(
        title = "Comparison Results",
        subtitle = if (results.isEmpty()) {
            "Run the benchmark to display model rankings and telemetry."
        } else {
            "Simulated latency, throughput, memory and quality for each selected model."
        },
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        if (results.isEmpty()) {
            Text(
                text = "No benchmark results yet.",
                style = AstraTypography.Body,
                color = AstraColors.TextSecondary,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.M)) {
                results.forEach { result ->
                    BenchmarkResultRow(
                        result = result,
                        recommended = result.model == recommendedModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun BenchmarkResultRow(
    result: BenchmarkResult,
    recommended: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (recommended) {
                    AstraColors.Primary.copy(alpha = 0.14f)
                } else {
                    AstraColors.SurfaceElevated
                },
                shape = RoundedCornerShape(20.dp),
            )
            .border(
                width = 1.dp,
                color = if (recommended) AstraColors.Secondary else AstraColors.Border,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(AstraSpacing.M),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.model.label,
                    style = AstraTypography.Title,
                    color = AstraColors.TextPrimary,
                )
                Text(
                    text = "${result.backend.label} • ${result.status.label}",
                    style = AstraTypography.Caption,
                    color = AstraColors.TextSecondary,
                )
            }
            if (recommended) {
                AstraChip(label = "RECOMMENDED", color = AstraColors.Success)
            }
        }
        Spacer(Modifier.height(AstraSpacing.M))
        Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                AstraMetricCard("${result.latencyMillis}", "ms", "Latency", Modifier.weight(1f))
                AstraMetricCard("${result.timeToFirstTokenMillis}", "ms", "TTFT", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                AstraMetricCard("${result.tokensPerSecond}", "", "Tokens/sec", Modifier.weight(1f))
                AstraMetricCard("${result.memoryUsageMb}", "MB", "Memory", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                AstraMetricCard("${result.qualityScore}", "/100", "Quality", Modifier.weight(1f))
                AstraMetricCard(result.status.label, "", "Status", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HorizontalOptions(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
    ) {
        content()
    }
}

@Composable
private fun SelectablePill(
    label: String,
    status: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .heightIn(min = 72.dp)
            .alpha(if (enabled) 1f else 0.58f)
            .background(
                color = if (selected) {
                    AstraColors.Secondary.copy(alpha = 0.14f)
                } else {
                    AstraColors.SurfaceElevated
                },
                shape = RoundedCornerShape(18.dp),
            )
            .border(
                width = 1.dp,
                color = if (selected) AstraColors.Secondary else AstraColors.Border,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(AstraSpacing.M),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = AstraTypography.Body,
            color = if (enabled || selected) AstraColors.TextPrimary else AstraColors.TextDisabled,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Spacer(Modifier.height(AstraSpacing.XS))
        Text(
            text = status,
            style = AstraTypography.Caption,
            color = if (enabled || selected) AstraColors.Secondary else AstraColors.TextDisabled,
        )
    }
}
