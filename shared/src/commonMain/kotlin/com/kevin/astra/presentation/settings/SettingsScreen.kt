package com.kevin.astra.presentation.settings

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.ai.BackendStatus
import com.kevin.astra.core.ai.InferenceBackendInfo
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.ModelStatus
import com.kevin.astra.core.ai.PromptIndustry
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
fun SettingsScreen(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsContent(
        state = state,
        contentPadding = contentPadding,
        onIntent = viewModel::dispatch,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    contentPadding: PaddingValues,
    onIntent: (SettingsIntent) -> Unit,
) {
    AstraScreen(
        title = "Settings",
        description = "Configure ASTRA's local AI runtime for secure offline operations.",
        contentPadding = contentPadding,
    ) {
        ModelConfigurationCard(
            models = state.availableModels,
            selectedModel = state.selectedModel,
            onSelectModel = { onIntent(SettingsIntent.SelectModel(it)) },
        )
        BackendConfigurationCard(
            backends = state.availableBackends,
            selectedBackend = state.selectedBackend,
            onSelectBackend = { onIntent(SettingsIntent.SelectBackend(it)) },
        )
        IndustryConfigurationCard(
            selectedIndustry = state.selectedIndustry,
            onSelectIndustry = { onIntent(SettingsIntent.SelectIndustry(it)) },
        )
        InferenceParametersCard(
            state = state,
            onIntent = onIntent,
        )
        ExperimentalFeaturesCard(
            enabled = state.experimentalFeaturesEnabled,
            onToggle = { onIntent(SettingsIntent.ToggleExperimentalFeatures(it)) },
        )
    }
}

@Composable
private fun ModelConfigurationCard(
    models: List<LocalModel>,
    selectedModel: LocalModel?,
    onSelectModel: (String) -> Unit,
) {
    AstraCard(
        title = "Model Configuration",
        subtitle = "Models are provided by the local catalog. Production runtimes remain staged for future integration.",
        status = selectedModel?.displayName ?: "No model",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        SelectableOptionRow {
            models.forEach { model ->
                val selected = model.id == selectedModel?.id
                val installed = model.status == ModelStatus.Installed
                SelectableOption(
                    label = model.displayName,
                    selected = selected,
                    enabled = installed,
                    status = if (selected) "ACTIVE" else model.status.label.uppercase(),
                    onClick = { onSelectModel(model.id) },
                )
            }
        }
    }
}

@Composable
private fun BackendConfigurationCard(
    backends: List<InferenceBackendInfo>,
    selectedBackend: InferenceBackendInfo?,
    onSelectBackend: (String) -> Unit,
) {
    AstraCard(
        title = "Backend Configuration",
        subtitle = "Backends are provided by the local catalog. Only installed runtimes can be selected.",
        status = selectedBackend?.displayName ?: "No backend",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        SelectableOptionRow {
            backends.forEach { backend ->
                val selected = backend.id == selectedBackend?.id
                val installed = backend.status == BackendStatus.Installed
                SelectableOption(
                    label = backend.displayName,
                    selected = selected,
                    enabled = installed,
                    status = if (selected) "ACTIVE" else backend.status.label.uppercase(),
                    onClick = { onSelectBackend(backend.id) },
                )
            }
        }
    }
}

@Composable
private fun IndustryConfigurationCard(
    selectedIndustry: PromptIndustry,
    onSelectIndustry: (PromptIndustry) -> Unit,
) {
    AstraCard(
        title = "Industry Persona",
        subtitle = "Default operational context used by the local assistant configuration.",
        status = selectedIndustry.label,
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        SelectableOptionRow {
            PromptIndustry.entries.forEach { industry ->
                SelectableOption(
                    label = industry.label,
                    selected = industry == selectedIndustry,
                    enabled = true,
                    status = if (industry == selectedIndustry) "SELECTED" else null,
                    onClick = { onSelectIndustry(industry) },
                )
            }
        }
    }
}

@Composable
private fun InferenceParametersCard(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    AstraCard(
        title = "Inference Parameters",
        subtitle = "In-memory runtime parameters used when ASTRA builds local prompt requests.",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraMetricCard(
                value = state.temperature.toString(),
                unit = "",
                label = "Temperature",
                modifier = Modifier.weight(1f),
            )
            AstraMetricCard(
                value = state.maxTokens.toString(),
                unit = "",
                label = "Max tokens",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(AstraSpacing.S))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraMetricCard(
                value = state.contextWindow.toString(),
                unit = "",
                label = "Context window",
                modifier = Modifier.weight(1f),
            )
            AstraMetricCard(
                value = state.quantization,
                unit = "",
                label = "Quantization",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(AstraSpacing.M))
        ParameterStepper(
            label = "Temperature",
            value = state.temperature.toString(),
            onDecrease = {
                onIntent(SettingsIntent.UpdateTemperature(roundParameter(state.temperature - 0.1)))
            },
            onIncrease = {
                onIntent(SettingsIntent.UpdateTemperature(roundParameter(state.temperature + 0.1)))
            },
        )
        ParameterStepper(
            label = "Max tokens",
            value = state.maxTokens.toString(),
            onDecrease = {
                onIntent(SettingsIntent.UpdateMaxTokens(state.maxTokens - 128))
            },
            onIncrease = {
                onIntent(SettingsIntent.UpdateMaxTokens(state.maxTokens + 128))
            },
        )
        ParameterStepper(
            label = "Context window",
            value = state.contextWindow.toString(),
            onDecrease = {
                onIntent(SettingsIntent.UpdateContextWindow(state.contextWindow - 1_024))
            },
            onIncrease = {
                onIntent(SettingsIntent.UpdateContextWindow(state.contextWindow + 1_024))
            },
        )
        Spacer(Modifier.height(AstraSpacing.S))
        Text(
            text = "Quantization",
            style = AstraTypography.Caption,
            color = AstraColors.TextSecondary,
        )
        Spacer(Modifier.height(AstraSpacing.S))
        SelectableOptionRow {
            listOf("4-bit", "8-bit", "16-bit").forEach { quantization ->
                SelectableOption(
                    label = quantization,
                    selected = quantization == state.quantization,
                    enabled = true,
                    status = if (quantization == state.quantization) "SELECTED" else null,
                    onClick = { onIntent(SettingsIntent.UpdateQuantization(quantization)) },
                )
            }
        }
    }
}

@Composable
private fun ExperimentalFeaturesCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    AstraCard(
        title = "Experimental Features",
        subtitle = "Prepare ASTRA for future runtime labs without enabling real inference backends yet.",
        status = if (enabled) "ENABLED" else "DISABLED",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AstraButton(
                text = if (enabled) "Disable experiments" else "Enable experiments",
                onClick = { onToggle(!enabled) },
                modifier = Modifier.weight(1f),
                style = if (enabled) AstraButtonStyle.Secondary else AstraButtonStyle.Primary,
            )
            AstraChip(
                label = if (enabled) "LAB MODE" else "STABLE",
                color = if (enabled) AstraColors.Warning else AstraColors.Success,
            )
        }
    }
}

@Composable
private fun SelectableOptionRow(content: @Composable () -> Unit) {
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
private fun SelectableOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    status: String?,
    onClick: () -> Unit,
) {
    val accent = when {
        selected -> AstraColors.Secondary
        enabled -> AstraColors.Border
        else -> AstraColors.TextDisabled
    }

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
            .border(1.dp, accent, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(AstraSpacing.M),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = AstraTypography.Body,
            color = if (enabled) AstraColors.TextPrimary else AstraColors.TextDisabled,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (status != null) {
            Spacer(Modifier.height(AstraSpacing.XS))
            Text(
                text = status,
                style = AstraTypography.Caption,
                color = if (enabled) AstraColors.Secondary else AstraColors.TextDisabled,
            )
        }
    }
}

@Composable
private fun ParameterStepper(
    label: String,
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AstraSpacing.XS),
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = AstraTypography.Caption,
                color = AstraColors.TextSecondary,
            )
            Text(
                text = value,
                style = AstraTypography.Body,
                color = AstraColors.TextPrimary,
            )
        }
        MiniControlButton(text = "−", onClick = onDecrease)
        MiniControlButton(text = "+", onClick = onIncrease)
    }
}

@Composable
private fun MiniControlButton(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .background(AstraColors.SurfaceElevated, RoundedCornerShape(16.dp))
            .border(1.dp, AstraColors.Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = AstraSpacing.L, vertical = AstraSpacing.S),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AstraTypography.Title,
            color = AstraColors.TextPrimary,
        )
    }
}

private fun roundParameter(value: Double): Double =
    (value * 10).toInt() / 10.0
