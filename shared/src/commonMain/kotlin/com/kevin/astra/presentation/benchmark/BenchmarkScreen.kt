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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.ai.BackendStatus
import com.kevin.astra.core.ai.InferenceBackendInfo
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.design.AstraButton
import com.kevin.astra.core.design.AstraButtonStyle
import com.kevin.astra.core.design.AstraCard
import com.kevin.astra.core.design.AstraChip
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraEmptyView
import com.kevin.astra.core.design.AstraErrorView
import com.kevin.astra.core.design.AstraMetricCard
import com.kevin.astra.core.design.AstraScreen
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import com.kevin.astra.domain.benchmark.BenchmarkResult
import com.kevin.astra.domain.benchmark.Co2Estimator
import com.kevin.astra.domain.demo.DemoScenario
import com.kevin.astra.domain.evaluation.TaskEvaluationCriterion

@Composable
fun BenchmarkScreen(
    contentPadding: PaddingValues,
    viewModel: BenchmarkViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.isRunning) {
        if (!state.isRunning && state.results.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

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
        description = "Compare on-device AI models — tokens/s, latency, RAM, battery, temperature.",
        contentPadding = contentPadding,
    ) {
        ScenarioSelector(
            scenarios = state.availableScenarios,
            onScenarioSelected = { onIntent(BenchmarkIntent.SelectScenario(it)) },
        )
        BenchmarkPromptCard(
            prompt = state.prompt,
            isRunning = state.isRunning,
            onPromptChanged = { onIntent(BenchmarkIntent.UpdatePrompt(it)) },
        )
        ModelSelectionCard(
            availableModels = state.availableModels,
            selectedModelIds = state.selectedModelIds,
            isRunning = state.isRunning,
            onToggleModel = { onIntent(BenchmarkIntent.ToggleModel(it)) },
            onSelectAll = { onIntent(BenchmarkIntent.SelectAllModels) },
            onClearAll = { onIntent(BenchmarkIntent.ClearModelSelection) },
        )
        BackendSelectionCard(
            backends = state.availableBackends,
            selectedBackend = state.selectedBackend,
            isRunning = state.isRunning,
            onSelectBackend = { onIntent(BenchmarkIntent.SelectBackend(it)) },
        )

        state.error?.let {
            AstraErrorView(title = "Benchmark Error", message = it)
        }

        RunBenchmarkCard(state = state, onRun = { onIntent(BenchmarkIntent.RunBenchmark) })

        AnimatedVisibility(visible = state.isRunning) {
            BenchmarkProgressCard(
                currentModel = state.currentlyBenchmarkingModel,
                completed = state.completedCount,
                total = state.totalToRun,
            )
        }

        state.recommendedModel?.let { recommendation ->
            AstraCard(
                title = "Recommended model",
                subtitle = recommendation.explanation,
                status = recommendation.model.displayName,
            ) {
                Spacer(Modifier.height(AstraSpacing.M))
                AstraChip(label = "BEST FIT", color = AstraColors.Success)
            }
        }

        if (state.results.size >= 2) {
            BenchmarkChartCard(results = state.results)
        }

        if (state.results.isNotEmpty()) {
            Co2SummaryCard(results = state.results)
        }

        ResultsCard(
            results = state.results,
            recommendedModelId = state.recommendedModel?.model?.id,
        )
    }
}

@Composable
private fun BenchmarkProgressCard(
    currentModel: String?,
    completed: Int,
    total: Int,
) {
    AstraCard(
        title = "Running…",
        subtitle = "Testing: ${currentModel ?: "—"}",
        status = "$completed / $total",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        LinearProgressIndicator(
            progress = { if (total > 0) completed.toFloat() / total else 0f },
            modifier = Modifier.fillMaxWidth(),
            color = AstraColors.Secondary,
            trackColor = AstraColors.Border,
        )
        Spacer(Modifier.height(AstraSpacing.S))
        Text(
            text = "Capturing tokens/s, latency, battery, temperature…",
            style = AstraTypography.Caption,
            color = AstraColors.TextSecondary,
        )
    }
}

@Composable
private fun ScenarioSelector(
    scenarios: List<DemoScenario>,
    onScenarioSelected: (DemoScenario) -> Unit,
) {
    AstraCard(
        title = "Benchmark Scenarios",
        subtitle = "Pre-defined prompts for standardized model evaluation.",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        ) {
            scenarios.forEach { scenario ->
                ScenarioChip(scenario = scenario, onClick = { onScenarioSelected(scenario) })
            }
        }
    }
}

@Composable
private fun ScenarioChip(scenario: DemoScenario, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 64.dp)
            .background(AstraColors.SurfaceElevated, RoundedCornerShape(16.dp))
            .border(1.dp, AstraColors.Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
        contentAlignment = Alignment.Center,
    ) {
        Column {
            Text(scenario.title, style = AstraTypography.Caption, color = AstraColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(scenario.industry.label, style = AstraTypography.Caption, color = AstraColors.TextSecondary)
        }
    }
}

@Composable
private fun BenchmarkPromptCard(prompt: String, isRunning: Boolean, onPromptChanged: (String) -> Unit) {
    AstraCard(title = "Benchmark Prompt", subtitle = "All selected models are evaluated against the same prompt.", status = "EDITABLE") {
        Spacer(Modifier.height(AstraSpacing.M))
        BasicTextField(
            value = prompt,
            onValueChange = onPromptChanged,
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).alpha(if (isRunning) 0.64f else 1f),
            textStyle = AstraTypography.Body.copy(color = AstraColors.TextPrimary),
            cursorBrush = SolidColor(AstraColors.Secondary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth().heightIn(min = 120.dp)
                        .background(AstraColors.SurfaceElevated, RoundedCornerShape(20.dp))
                        .border(1.dp, AstraColors.Border, RoundedCornerShape(20.dp))
                        .padding(AstraSpacing.M),
                ) {
                    if (prompt.isBlank()) {
                        Text(DefaultBenchmarkPrompt, style = AstraTypography.Body, color = AstraColors.TextDisabled)
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun ModelSelectionCard(
    availableModels: List<LocalModel>,
    selectedModelIds: Set<String>,
    isRunning: Boolean,
    onToggleModel: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
) {
    AstraCard(
        title = "Model Selection",
        subtitle = "Select models to benchmark. All run sequentially on-device.",
        status = "${selectedModelIds.size} SELECTED",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraButton(
                text = "Select All",
                onClick = onSelectAll,
                enabled = !isRunning && selectedModelIds.size < availableModels.size,
                style = AstraButtonStyle.Ghost,
            )
            AstraButton(
                text = "Clear",
                onClick = onClearAll,
                enabled = !isRunning && selectedModelIds.isNotEmpty(),
                style = AstraButtonStyle.Ghost,
            )
        }
        Spacer(Modifier.height(AstraSpacing.S))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        ) {
            availableModels.forEach { model ->
                val selected = model.id in selectedModelIds
                SelectablePill(
                    label = model.displayName,
                    status = if (selected) "SELECTED" else model.status.label.uppercase(),
                    selected = selected,
                    enabled = !isRunning,
                    onClick = { onToggleModel(model.id) },
                )
            }
        }
    }
}

@Composable
private fun BackendSelectionCard(
    backends: List<InferenceBackendInfo>,
    selectedBackend: InferenceBackendInfo?,
    isRunning: Boolean,
    onSelectBackend: (String) -> Unit,
) {
    AstraCard(
        title = "Backend",
        subtitle = "Runtime backend for all models in this benchmark session.",
        status = selectedBackend?.displayName ?: "None",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        ) {
            backends.forEach { backend ->
                val selected = backend.id == selectedBackend?.id
                SelectablePill(
                    label = backend.displayName,
                    status = if (selected) "ACTIVE" else backend.status.label.uppercase(),
                    selected = selected,
                    enabled = backend.status == BackendStatus.Installed && !isRunning,
                    onClick = { onSelectBackend(backend.id) },
                )
            }
        }
    }
}

@Composable
private fun RunBenchmarkCard(state: BenchmarkState, onRun: () -> Unit) {
    AstraCard(
        title = "Execution",
        subtitle = "Models are tested sequentially. Hardware metrics captured per run.",
        status = if (state.isRunning) "RUNNING" else "READY",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraMetricCard(state.selectedModelIds.size.toString(), "", "Models", Modifier.weight(1f))
            AstraMetricCard(state.results.size.toString(), "", "Completed", Modifier.weight(1f))
        }
        Spacer(Modifier.height(AstraSpacing.M))
        AstraButton(
            text = if (state.isRunning) "Running…" else "Run Benchmark",
            onClick = onRun,
            enabled = state.canRun,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ResultsCard(results: List<BenchmarkResult>, recommendedModelId: String?) {
    AstraCard(
        title = "Results",
        subtitle = if (results.isEmpty()) "Run the benchmark to see model rankings." else "${results.size} model(s) evaluated.",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        if (results.isEmpty()) {
            AstraEmptyView(title = "No results", message = "Select models and tap 'Run Benchmark'.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.M)) {
                results.forEach { result ->
                    BenchmarkResultRow(result = result, recommended = result.model.id == recommendedModelId)
                }
            }
        }
    }
}

@Composable
private fun BenchmarkResultRow(result: BenchmarkResult, recommended: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (recommended) AstraColors.Primary.copy(alpha = 0.14f) else AstraColors.SurfaceElevated,
                RoundedCornerShape(20.dp),
            )
            .border(1.dp, if (recommended) AstraColors.Secondary else AstraColors.Border, RoundedCornerShape(20.dp))
            .padding(AstraSpacing.M),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(result.model.displayName, style = AstraTypography.Title, color = AstraColors.TextPrimary)
                Text(
                    "${result.model.provider.label} • ${result.usedBackend.label} • ${result.status.label}",
                    style = AstraTypography.Caption, color = AstraColors.TextSecondary,
                )
            }
            if (recommended) AstraChip(label = "BEST", color = AstraColors.Success)
        }
        Spacer(Modifier.height(AstraSpacing.M))
        Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                AstraMetricCard(result.tokensPerSecond.displayNum(), "tok/s", "Tokens/s", Modifier.weight(1f))
                AstraMetricCard(result.latencyMillis.displayNum(), result.latencyMillis.displayUnit("ms"), "Latency", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                AstraMetricCard(result.timeToFirstTokenMillis.displayNum(), result.timeToFirstTokenMillis.displayUnit("ms"), "TTFT", Modifier.weight(1f))
                AstraMetricCard(result.memoryUsageMb.displayNum(), result.memoryUsageMb.displayUnit("MB"), "RAM", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                AstraMetricCard("${result.taskEvaluation.overallScore}", "/100", "Task Score", Modifier.weight(1f))
                AstraMetricCard(result.runtimeInfo.mode.label, "", "Runtime", Modifier.weight(1f))
            }
            // Hardware metrics
            val battery = result.batteryDrainPercent
            val temp = result.peakTemperatureCelsius
            val tempDelta = result.temperatureDeltaCelsius
            if (battery != null || temp != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                    AstraMetricCard(
                        battery?.let { "$it" } ?: "N/A",
                        battery?.let { "%" } ?: "",
                        "Battery drain",
                        Modifier.weight(1f),
                    )
                    AstraMetricCard(
                        temp?.let { "${(it * 10).toInt() / 10f}" } ?: "N/A",
                        temp?.let { "°C" } ?: "",
                        "Peak temp" + (tempDelta?.let { d -> " (+${(d * 10).toInt() / 10f})" } ?: ""),
                        Modifier.weight(1f),
                    )
                }
            }
            // CO₂ metrics
            val co2OnDevice = result.onDeviceCo2Mg
            val co2Cloud = result.cloudEquivalentCo2Mg
            val co2Savings = result.co2SavingsPercent
            if (co2OnDevice != null || co2Cloud != null) {
                Co2MetricsRow(onDeviceMg = co2OnDevice, cloudMg = co2Cloud, savingsPercent = co2Savings)
            }

            TaskEvaluationBreakdown(result = result)
            result.runtimeInfo.fallbackReason?.let { reason ->
                AstraErrorView(
                    title = "Fallback",
                    message = "Selected: ${result.selectedBackend.label} → Used: ${result.usedBackend.label}\n$reason",
                )
            }
        }
    }
}

@Composable
private fun Co2SummaryCard(results: List<BenchmarkResult>) {
    val totalOnDeviceMg = results.mapNotNull { it.onDeviceCo2Mg }.sum()
    val totalCloudMg = results.mapNotNull { it.cloudEquivalentCo2Mg }.sum()
    if (totalOnDeviceMg == 0.0 && totalCloudMg == 0.0) return

    val savingsPercent = Co2Estimator.savingsPercent(totalOnDeviceMg, totalCloudMg)

    AstraCard(
        title = "Carbon Impact",
        subtitle = "Estimated CO₂ for this benchmark session vs equivalent cloud inference.",
        status = if (savingsPercent > 0) "−$savingsPercent% CO₂" else "N/A",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraMetricCard(
                value = Co2Estimator.display(totalOnDeviceMg).substringBefore(" "),
                unit = if (totalOnDeviceMg < 1_000) "mg CO₂" else "g CO₂",
                label = "On-device total",
                modifier = Modifier.weight(1f),
            )
            AstraMetricCard(
                value = Co2Estimator.display(totalCloudMg).substringBefore(" "),
                unit = if (totalCloudMg < 1_000) "mg CO₂" else "g CO₂",
                label = "Cloud equiv.",
                modifier = Modifier.weight(1f),
            )
        }
        if (savingsPercent > 0) {
            Spacer(Modifier.height(AstraSpacing.S))
            Text(
                text = "Running ASTRA on-device saved an estimated $savingsPercent% of the CO₂ that equivalent cloud inference would have produced.",
                style = AstraTypography.Caption,
                color = AstraColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun Co2MetricsRow(
    onDeviceMg: Double?,
    cloudMg: Double?,
    savingsPercent: Int?,
) {
    Spacer(Modifier.height(AstraSpacing.S))
    Text("Carbon Footprint", style = AstraTypography.Caption, color = AstraColors.TextSecondary)
    Spacer(Modifier.height(AstraSpacing.S))
    Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
        AstraMetricCard(
            value = onDeviceMg?.let { Co2Estimator.display(it).substringBefore(" ") } ?: "N/A",
            unit = onDeviceMg?.let { if (it < 1_000) "mg CO₂" else "g CO₂" } ?: "",
            label = "On-device",
            modifier = Modifier.weight(1f),
        )
        AstraMetricCard(
            value = cloudMg?.let { Co2Estimator.display(it).substringBefore(" ") } ?: "N/A",
            unit = cloudMg?.let { if (it < 1_000) "mg CO₂" else "g CO₂" } ?: "",
            label = "Cloud equiv.",
            modifier = Modifier.weight(1f),
        )
        if (savingsPercent != null) {
            AstraMetricCard(
                value = "$savingsPercent",
                unit = "%",
                label = "CO₂ saved",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TaskEvaluationBreakdown(result: BenchmarkResult) {
    Spacer(Modifier.height(AstraSpacing.S))
    Text("Task Evaluation", style = AstraTypography.Caption, color = AstraColors.TextSecondary)
    Spacer(Modifier.height(AstraSpacing.S))
    Text(result.taskEvaluation.recommendationSummary, style = AstraTypography.Caption, color = AstraColors.TextPrimary)
    Spacer(Modifier.height(AstraSpacing.S))
    Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
        EvaluationCriterionRow(result, TaskEvaluationCriterion.Safety)
        EvaluationCriterionRow(result, TaskEvaluationCriterion.ProcedureCompleteness)
        EvaluationCriterionRow(result, TaskEvaluationCriterion.TechnicalAccuracy)
        EvaluationCriterionRow(result, TaskEvaluationCriterion.DomainTerminology)
        EvaluationCriterionRow(result, TaskEvaluationCriterion.Clarity)
    }
}

@Composable
private fun EvaluationCriterionRow(result: BenchmarkResult, criterion: TaskEvaluationCriterion) {
    val score = result.taskEvaluation.breakdown.scoreFor(criterion)
    Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
        AstraMetricCard("${score.score}", "/100", criterion.label, Modifier.weight(1f))
        AstraMetricCard("${criterion.weight}", "%", "Weight", Modifier.weight(1f))
    }
}

private fun Int?.displayNum(): String = this?.toString() ?: "N/A"
private fun Int?.displayUnit(unit: String): String = if (this == null) "" else unit
private fun Long?.displayNum(): String = this?.toString() ?: "N/A"
private fun Long?.displayUnit(unit: String): String = if (this == null) "" else unit

@Composable
private fun HorizontalOptions(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
    ) { content() }
}

@Composable
private fun SelectablePill(label: String, status: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .heightIn(min = 72.dp)
            .alpha(if (enabled) 1f else 0.58f)
            .background(
                if (selected) AstraColors.Secondary.copy(alpha = 0.14f) else AstraColors.SurfaceElevated,
                RoundedCornerShape(18.dp),
            )
            .border(1.dp, if (selected) AstraColors.Secondary else AstraColors.Border, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(AstraSpacing.M),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, style = AstraTypography.Body, color = if (enabled || selected) AstraColors.TextPrimary else AstraColors.TextDisabled, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        Spacer(Modifier.height(AstraSpacing.XS))
        Text(status, style = AstraTypography.Caption, color = if (enabled || selected) AstraColors.Secondary else AstraColors.TextDisabled)
    }
}
