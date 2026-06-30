package com.kevin.astra.presentation.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.ModelStatus
import com.kevin.astra.core.design.AstraButton
import com.kevin.astra.core.design.AstraButtonStyle
import com.kevin.astra.core.design.AstraCard
import com.kevin.astra.core.design.AstraChip
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraErrorView
import com.kevin.astra.core.design.AstraMetricCard
import com.kevin.astra.core.design.AstraScreen
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import com.kevin.astra.core.design.DemoModeBanner
import com.kevin.astra.domain.assistant.PromptTemplate
import com.kevin.astra.domain.demo.DemoScenario
import com.kevin.astra.domain.settings.DemoModeHolder

@Composable
fun AssistantScreen(
    contentPadding: PaddingValues,
    viewModel: AssistantViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.isGenerating) {
        if (!state.isGenerating && state.response != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    AssistantContent(
        state = state,
        contentPadding = contentPadding,
        onIntent = viewModel::dispatch,
    )
}

@Composable
private fun AssistantContent(
    state: AssistantState,
    contentPadding: PaddingValues,
    onIntent: (AssistantIntent) -> Unit,
) {
    val isDemoMode by DemoModeHolder.enabled.collectAsStateWithLifecycle()

    AstraScreen(
        title = "ASTRA Assistant",
        description = "Secure Local AI for Critical Operations",
        contentPadding = contentPadding,
    ) {
        if (isDemoMode) DemoModeBanner()

        if (state.installedModels.size > 1) {
            ModelSelector(
                models = state.installedModels,
                selectedModel = state.sessionModel,
                enabled = !state.isGenerating,
                onSelectModel = { onIntent(AssistantIntent.SelectSessionModel(it)) },
            )
        }

        PromptTemplateCard(
            templates = state.promptTemplates,
            activeTemplate = state.activeTemplate,
            enabled = !state.isGenerating,
            onTemplateSelected = { onIntent(AssistantIntent.SelectTemplate(it)) },
        )

        ScenarioSelector(
            scenarios = state.availableScenarios,
            onScenarioSelected = { onIntent(AssistantIntent.SelectScenario(it)) },
        )

        IndustrySelector(
            selectedIndustry = state.selectedIndustry,
            enabled = !state.isGenerating,
            onIndustrySelected = { onIntent(AssistantIntent.SelectIndustry(it)) },
        )

        AnimatedVisibility(visible = state.isGenerating && !state.isStreaming) {
            LoadingCard()
        }

        PromptCard(
            question = state.question,
            isGenerating = state.isGenerating,
            canAsk = state.canAsk,
            error = state.error,
            onQuestionChanged = { onIntent(AssistantIntent.UpdateQuestion(it)) },
            onAsk = { onIntent(AssistantIntent.AskQuestion) },
            onClear = { onIntent(AssistantIntent.ClearConversation) },
        )

        AnimatedVisibility(visible = state.isStreaming) {
            StreamingResponseCard(text = state.streamingText)
        }

        AnimatedVisibility(visible = state.response != null && !state.isGenerating) {
            state.response?.let { response ->
                Column {
                    AssistantResponseCard(
                        response = response,
                        timestamp = state.generationTimestamp.orEmpty(),
                        metrics = state.metrics,
                    )
                    MetricsPanel(metrics = state.metrics)
                }
            }
        }
    }
}

@Composable
private fun ScenarioSelector(
    scenarios: List<DemoScenario>,
    onScenarioSelected: (DemoScenario) -> Unit,
) {
    AstraCard(
        title = "Demo Scenarios",
        subtitle = "Curated prompts for engineering demonstration.",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        ) {
            scenarios.forEach { scenario ->
                ScenarioChip(
                    scenario = scenario,
                    onClick = { onScenarioSelected(scenario) },
                )
            }
        }
    }
}

@Composable
private fun ScenarioChip(
    scenario: DemoScenario,
    onClick: () -> Unit,
) {
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
            Text(
                text = scenario.title,
                style = AstraTypography.Caption,
                color = AstraColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = scenario.industry.label,
                style = AstraTypography.Caption,
                color = AstraColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun ModelSelector(
    models: List<LocalModel>,
    selectedModel: LocalModel?,
    enabled: Boolean,
    onSelectModel: (String) -> Unit,
) {
    AstraCard(
        title = "Model",
        subtitle = "Active model for this session. Switch without changing your saved settings.",
        status = selectedModel?.displayName ?: "—",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        ) {
            models.forEach { model ->
                val selected = model.id == selectedModel?.id
                val accent = if (selected) AstraColors.Primary else AstraColors.Border
                Box(
                    modifier = Modifier
                        .heightIn(min = 64.dp)
                        .alpha(if (enabled) 1f else 0.54f)
                        .background(
                            color = if (selected) AstraColors.Primary.copy(alpha = 0.14f) else AstraColors.SurfaceElevated,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .border(1.dp, accent, RoundedCornerShape(16.dp))
                        .clickable(enabled = enabled) { onSelectModel(model.id) }
                        .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
                    contentAlignment = Alignment.Center,
                ) {
                    Column {
                        Text(
                            text = model.displayName,
                            style = AstraTypography.Caption,
                            color = if (selected) AstraColors.TextPrimary else AstraColors.TextSecondary,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Text(
                            text = "${model.parameterCount} • ${model.quantization}",
                            style = AstraTypography.Caption,
                            color = AstraColors.TextDisabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptTemplateCard(
    templates: List<PromptTemplate>,
    activeTemplate: PromptTemplate?,
    enabled: Boolean,
    onTemplateSelected: (PromptTemplate) -> Unit,
) {
    AstraCard(
        title = "Prompt Templates",
        subtitle = "One tap to pre-fill the prompt with a structured starting point.",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        ) {
            templates.forEach { template ->
                val selected = template.id == activeTemplate?.id
                val accent = if (selected) AstraColors.Secondary else AstraColors.Border
                Box(
                    modifier = Modifier
                        .heightIn(min = 64.dp)
                        .alpha(if (enabled) 1f else 0.54f)
                        .background(
                            color = if (selected) AstraColors.Secondary.copy(alpha = 0.14f) else AstraColors.SurfaceElevated,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .border(1.dp, accent, RoundedCornerShape(16.dp))
                        .clickable(enabled = enabled) { onTemplateSelected(template) }
                        .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = template.icon,
                            style = AstraTypography.Title,
                            color = if (selected) AstraColors.Secondary else AstraColors.TextSecondary,
                        )
                        Spacer(Modifier.height(AstraSpacing.XS))
                        Text(
                            text = template.label,
                            style = AstraTypography.Caption,
                            color = if (selected) AstraColors.TextPrimary else AstraColors.TextSecondary,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IndustrySelector(
    selectedIndustry: AssistantIndustry,
    enabled: Boolean,
    onIndustrySelected: (AssistantIndustry) -> Unit,
) {
    AstraCard(
        title = "Industry",
        subtitle = "Tune the future local assistant to the current operational context.",
        status = selectedIndustry.label,
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        ) {
            AssistantIndustry.entries.forEach { industry ->
                IndustryChip(
                    industry = industry,
                    selected = industry == selectedIndustry,
                    enabled = enabled,
                    onClick = { onIndustrySelected(industry) },
                )
            }
        }
    }
}

@Composable
private fun IndustryChip(
    industry: AssistantIndustry,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val accent = if (selected) AstraColors.Secondary else AstraColors.Border
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.54f)
            .background(
                color = if (selected) AstraColors.Secondary.copy(alpha = 0.14f) else AstraColors.SurfaceElevated,
                shape = RoundedCornerShape(16.dp),
            )
            .border(1.dp, accent, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = industry.label,
            style = AstraTypography.Caption,
            color = if (selected) AstraColors.TextPrimary else AstraColors.TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun PromptCard(
    question: String,
    isGenerating: Boolean,
    canAsk: Boolean,
    error: String?,
    onQuestionChanged: (String) -> Unit,
    onAsk: () -> Unit,
    onClear: () -> Unit,
) {
    AstraCard(
        title = "Prompt",
        subtitle = "Ask ASTRA about a critical operation. ASTRA uses the selected local runtime and falls back only when required.",
        status = "LOCAL AI",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        AstraQuestionField(
            value = question,
            enabled = !isGenerating,
            onValueChange = onQuestionChanged,
        )

        AnimatedVisibility(visible = error != null) {
            error?.let {
                Spacer(Modifier.height(AstraSpacing.M))
                AstraErrorView(
                    title = "Assistant Error",
                    message = it,
                )
            }
        }

        Spacer(Modifier.height(AstraSpacing.M))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        ) {
            AstraButton(
                text = "Ask ASTRA",
                onClick = onAsk,
                modifier = Modifier.weight(1f),
                enabled = canAsk,
            )
            AstraButton(
                text = "Clear",
                onClick = onClear,
                style = AstraButtonStyle.Ghost,
                enabled = !isGenerating && question.isNotBlank(),
            )
        }
    }
}

@Composable
private fun AstraQuestionField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 148.dp)
            .alpha(if (enabled) 1f else 0.68f),
        enabled = enabled,
        textStyle = AstraTypography.Body.copy(color = AstraColors.TextPrimary),
        cursorBrush = SolidColor(AstraColors.Secondary),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 148.dp)
                    .background(AstraColors.SurfaceElevated, RoundedCornerShape(20.dp))
                    .border(1.dp, AstraColors.Border, RoundedCornerShape(20.dp))
                    .padding(AstraSpacing.M),
            ) {
                if (value.isBlank()) {
                    Text(
                        text = "Ask ASTRA about a critical operation...",
                        style = AstraTypography.Body,
                        color = AstraColors.TextDisabled,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun LoadingCard() {
    AstraCard(
        title = "Generating local response...",
        subtitle = "Controls are locked while ASTRA runs the selected local inference path.",
        status = "RUNNING",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.M),
        ) {
            AstraPulseIndicator()
            Text(
                text = "Generating local response...",
                style = AstraTypography.Body,
                color = AstraColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun AstraPulseIndicator() {
    val transition = rememberInfiniteTransition(label = "astra-loading")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "astra-loading-alpha",
    )

    Box(
        modifier = Modifier
            .size(18.dp)
            .alpha(alpha)
            .background(AstraColors.Secondary, RoundedCornerShape(50)),
    )
}

@Composable
private fun StreamingResponseCard(text: String) {
    val transition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "cursor-alpha",
    )
    AstraCard(title = "ASTRA is responding…", status = "STREAMING") {
        Spacer(Modifier.height(AstraSpacing.M))
        Row {
            Text(
                text = text,
                style = AstraTypography.Body,
                color = AstraColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "▌",
                style = AstraTypography.Body,
                color = AstraColors.Secondary,
                modifier = Modifier.alpha(cursorAlpha),
            )
        }
    }
}

@Composable
private fun AssistantResponseCard(
    response: AssistantResponse,
    timestamp: String,
    metrics: AssistantMetrics,
) {
    AstraCard(
        title = response.title,
        subtitle = "Generated by ${metrics.backend} • $timestamp",
        status = metrics.runtimeMode,
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Text(
            text = response.body,
            style = AstraTypography.Body,
            color = AstraColors.TextPrimary,
        )
    }
}

@Composable
private fun MetricsPanel(metrics: AssistantMetrics) {
    AstraCard(
        title = "Metrics",
        subtitle = "Runtime telemetry for local inference and fallback diagnostics.",
        status = metrics.runtimeMode,
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                AstraMetricCard(
                    value = metrics.model,
                    unit = "",
                    label = "Model",
                    modifier = Modifier.weight(1f),
                )
                AstraMetricCard(
                    value = metrics.backend,
                    unit = "",
                    label = "Backend",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                AstraMetricCard(
                    value = metrics.latency,
                    unit = "",
                    label = "Latency",
                    modifier = Modifier.weight(1f),
                )
                AstraMetricCard(
                    value = metrics.tokensPerSecond,
                    unit = "",
                    label = "Tokens/sec",
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                AstraMetricCard(
                    value = metrics.modelLoadTime,
                    unit = "",
                    label = "Model load",
                    modifier = Modifier.weight(1f),
                )
                AstraMetricCard(
                    value = metrics.totalExecutionTime,
                    unit = "",
                    label = "Total",
                    modifier = Modifier.weight(1f),
                )
            }
            metrics.fallbackReason?.let { reason ->
                AstraErrorView(
                    title = "Fallback active",
                    message = reason,
                )
            }
        }
        Spacer(Modifier.height(AstraSpacing.S))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraChip(label = "Offline mode", color = AstraColors.Success)
            AstraChip(label = metrics.runtimeMode, color = AstraColors.Secondary)
        }
    }
}
