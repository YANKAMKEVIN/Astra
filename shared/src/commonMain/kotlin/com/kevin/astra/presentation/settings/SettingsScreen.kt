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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.ai.BackendStatus
import com.kevin.astra.core.ai.InferenceBackendInfo
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.ModelStatus
import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.domain.modelmanager.ModelDownloadState
import com.kevin.astra.core.design.AstraButton
import com.kevin.astra.core.design.AstraButtonStyle
import com.kevin.astra.core.design.AstraCard
import com.kevin.astra.core.design.AstraChip
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraIcons
import com.kevin.astra.core.design.AstraMetricCard
import com.kevin.astra.core.design.AstraScreen
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.kevin.astra.domain.modelmanager.ModelReadiness
import com.kevin.astra.presentation.models.rememberDownloadPaceText
import com.kevin.astra.domain.settings.ThemeMode
import com.kevin.astra.domain.modelmanager.ModelReadinessStatus
import com.kevin.astra.domain.modelmanager.RequiredModelFile

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel,
    onManageModels: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is SettingsEffect.DownloadCompleted -> snackbarHostState.showSnackbar("✓ ${effect.modelName} downloaded successfully")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SettingsContent(
            state = state,
            contentPadding = contentPadding,
            onIntent = viewModel::dispatch,
            onManageModels = onManageModels,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
            snackbar = { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = AstraColors.SurfaceElevated,
                    contentColor = AstraColors.TextPrimary,
                    actionColor = AstraColors.Primary,
                )
            },
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    contentPadding: PaddingValues,
    onIntent: (SettingsIntent) -> Unit,
    onManageModels: () -> Unit,
) {
    AstraScreen(
        title = "Settings",
        description = "Configure ASTRA's local AI runtime for secure offline operations.",
        contentPadding = contentPadding,
    ) {
        ActiveModelCard(
            selectedModel = state.selectedModel,
            installedCount = state.modelReadiness.count { it.status == ModelReadinessStatus.Installed },
            totalCount = state.modelReadiness.size,
            downloadState = state.downloadState,
            onManageModels = onManageModels,
        )
        BackendConfigurationCard(
            backends = state.availableBackends,
            selectedBackend = state.selectedBackend,
            onSelectBackend = { onIntent(SettingsIntent.SelectBackend(it)) },
        )
        HuggingFaceTokenCard(
            token = state.huggingFaceToken,
            onTokenChange = { onIntent(SettingsIntent.UpdateHuggingFaceToken(it)) },
        )
        IndustryConfigurationCard(
            selectedIndustry = state.selectedIndustry,
            onSelectIndustry = { onIntent(SettingsIntent.SelectIndustry(it)) },
        )

        InferenceParametersCard(
            state = state,
            onIntent = onIntent,
        )
        DemoModeCard(
            enabled = state.demoModeEnabled,
            onToggle = { onIntent(SettingsIntent.ToggleDemoMode(it)) },
        )
        ThemeCard(
            mode = state.themeMode,
            onSelect = { onIntent(SettingsIntent.SelectThemeMode(it)) },
        )
        ExperimentalFeaturesCard(
            enabled = state.experimentalFeaturesEnabled,
            onToggle = { onIntent(SettingsIntent.ToggleExperimentalFeatures(it)) },
        )
    }
}

@Composable
private fun ActiveModelCard(
    selectedModel: LocalModel?,
    installedCount: Int,
    totalCount: Int,
    downloadState: ModelDownloadState,
    onManageModels: () -> Unit,
) {
    val downloading = downloadState as? ModelDownloadState.Downloading
    val pace = rememberDownloadPaceText(downloading)
    AstraCard(
        title = "Active model",
        subtitle = when {
            downloading != null -> "A model is downloading — ${downloading.progressPercent}%" +
                (pace?.let { " · $it" } ?: "") + ". Follow it in Models."
            else -> "$installedCount of $totalCount models on this device. Download, switch or delete them in Models."
        },
        status = selectedModel?.displayName ?: "No model",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        AstraButton(
            text = "Manage models",
            onClick = onManageModels,
            style = AstraButtonStyle.Secondary,
            leadingIcon = AstraIcons.Cube,
        )
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
private fun HuggingFaceTokenCard(
    token: String,
    onTokenChange: (String) -> Unit,
) {
    var masked by remember { mutableStateOf(true) }
    AstraCard(
        title = "HuggingFace Token",
        subtitle = "Required to download gated models (Gemma). Generate a token at huggingface.co/settings/tokens.",
        status = if (token.isNotBlank()) "Configured" else "Not set",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AstraColors.Surface, RoundedCornerShape(8.dp))
                .border(1.dp, AstraColors.Border, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = token,
                onValueChange = onTokenChange,
                singleLine = true,
                textStyle = AstraTypography.Body.copy(color = AstraColors.TextPrimary),
                visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (token.isEmpty()) {
                        Text(
                            text = "hf_...",
                            style = AstraTypography.Body,
                            color = AstraColors.TextSecondary,
                        )
                    }
                    inner()
                },
            )
            Text(
                text = if (masked) "Show" else "Hide",
                style = AstraTypography.Caption,
                color = AstraColors.Primary,
                modifier = Modifier.clickable { masked = !masked },
            )
        }
        if (token.isNotBlank()) {
            Spacer(Modifier.height(AstraSpacing.S))
            Text(
                text = "Token saved. Gemma models can now be downloaded.",
                style = AstraTypography.Caption,
                color = AstraColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun IndustryConfigurationCard(
    selectedIndustry: PromptIndustry?,
    onSelectIndustry: (PromptIndustry?) -> Unit,
) {
    AstraCard(
        title = "Industry Persona",
        subtitle = "Tailors answers to a sector. Tap the selected sector again for general mode.",
        status = selectedIndustry?.label ?: "None",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        if (selectedIndustry == null) {
            Text(
                text = "✦ General mode — no sector persona. Pick a sector below to tailor the answers.",
                style = AstraTypography.Caption,
                color = AstraColors.TextSecondary,
            )
            Spacer(Modifier.height(AstraSpacing.S))
        }
        SelectableOptionRow {
            PromptIndustry.entries.forEach { industry ->
                val isSelected = industry == selectedIndustry
                SelectableOption(
                    label = industry.label,
                    selected = isSelected,
                    enabled = true,
                    status = if (isSelected) "SELECTED" else null,
                    // tap selected → deselect (null), tap other → select it
                    onClick = { onSelectIndustry(if (isSelected) null else industry) },
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
        val scale = InferenceParameterScale
        ParameterSlider(
            label = "Temperature",
            description = "Controls randomness of the output. Low = focused, high = creative.",
            valueText = "${state.temperature} · ${scale.temperatureLabel(state.temperature)}",
            value = state.temperature.toFloat(),
            valueRange = scale.TemperatureMin.toFloat()..scale.TemperatureMax.toFloat(),
            steps = 9,
            onValueChange = { raw ->
                val snapped = scale.snapTemperature(raw)
                if (snapped != state.temperature) onIntent(SettingsIntent.UpdateTemperature(snapped))
            },
            presets = scale.TemperaturePresets.map { (name, value) ->
                ParameterPreset(name, selected = scale.temperatureLabel(state.temperature) == name) {
                    onIntent(SettingsIntent.UpdateTemperature(value))
                }
            },
        )
        ParameterSlider(
            label = "Max tokens",
            description = "Maximum number of tokens the model will generate per response.",
            valueText = formatTokens(state.maxTokens),
            value = state.maxTokens.toFloat(),
            valueRange = scale.MaxTokensMin.toFloat()..scale.MaxTokensMax.toFloat(),
            steps = scale.sliderSteps(scale.MaxTokensMin, scale.MaxTokensMax, scale.MaxTokensStep),
            onValueChange = { raw ->
                val snapped = scale.snap(raw, scale.MaxTokensMin, scale.MaxTokensMax, scale.MaxTokensStep)
                if (snapped != state.maxTokens) onIntent(SettingsIntent.UpdateMaxTokens(snapped))
            },
            presets = scale.MaxTokensPresets.map { tokens ->
                ParameterPreset(formatTokens(tokens), selected = state.maxTokens == tokens) {
                    onIntent(SettingsIntent.UpdateMaxTokens(tokens))
                }
            },
        )
        ParameterSlider(
            label = "Context window",
            description = "Total tokens (prompt + response) the model can process at once.",
            valueText = formatTokens(state.contextWindow),
            value = state.contextWindow.toFloat(),
            valueRange = scale.ContextWindowMin.toFloat()..scale.ContextWindowMax.toFloat(),
            steps = scale.sliderSteps(scale.ContextWindowMin, scale.ContextWindowMax, scale.ContextWindowStep),
            onValueChange = { raw ->
                val snapped = scale.snap(raw, scale.ContextWindowMin, scale.ContextWindowMax, scale.ContextWindowStep)
                if (snapped != state.contextWindow) onIntent(SettingsIntent.UpdateContextWindow(snapped))
            },
        )
        TextButton(
            onClick = { onIntent(SettingsIntent.ResetInferenceParameters) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Reset defaults", color = AstraColors.Secondary, style = AstraTypography.Caption)
        }
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
private fun DemoModeCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    AstraCard(
        title = "Offline Demo Mode",
        subtitle = "Forces all inference through the built-in mock engine — no model file required. Ideal for demos without a loaded model.",
        status = if (enabled) "ACTIVE" else "OFF",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AstraButton(
                text = if (enabled) "Disable demo mode" else "Enable demo mode",
                onClick = { onToggle(!enabled) },
                modifier = Modifier.weight(1f),
                style = if (enabled) AstraButtonStyle.Danger else AstraButtonStyle.Primary,
            )
            AstraChip(
                label = if (enabled) "MOCK" else "REAL",
                color = if (enabled) AstraColors.Warning else AstraColors.Success,
            )
        }
    }
}

@Composable
private fun ThemeCard(
    mode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    AstraCard(
        title = "Theme",
        subtitle = "Follow the phone's setting, or keep ASTRA dark or light. Dark is the default for edge environments.",
        status = mode.label.uppercase(),
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        SelectableOptionRow {
            ThemeMode.entries.forEach { option ->
                SelectableOption(
                    label = option.label,
                    selected = option == mode,
                    enabled = true,
                    status = if (option == mode) "SELECTED" else null,
                    onClick = { onSelect(option) },
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

private class ParameterPreset(val label: String, val selected: Boolean, val onClick: () -> Unit)

@Composable
private fun ParameterSlider(
    label: String,
    description: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    presets: List<ParameterPreset> = emptyList(),
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AstraSpacing.XS),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = AstraTypography.Body, color = AstraColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(text = description, style = AstraTypography.Caption, color = AstraColors.TextSecondary)
            }
            Text(text = valueText, style = AstraTypography.Metric, color = AstraColors.Secondary)
        }
        Slider(
            value = value.coerceIn(valueRange),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.semantics { contentDescription = label },
            colors = SliderDefaults.colors(
                thumbColor = AstraColors.Secondary,
                activeTrackColor = AstraColors.Primary,
                inactiveTrackColor = AstraColors.Border,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        if (presets.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                presets.forEach { preset -> PresetChip(preset) }
            }
        }
        Spacer(Modifier.height(AstraSpacing.S))
    }
}

@Composable
private fun PresetChip(preset: ParameterPreset) {
    val accent = if (preset.selected) AstraColors.Secondary else AstraColors.Border
    Box(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(50))
            .background(if (preset.selected) AstraColors.Secondary.copy(alpha = 0.14f) else AstraColors.SurfaceElevated)
            .border(1.dp, accent, RoundedCornerShape(50))
            .selectable(selected = preset.selected, role = Role.RadioButton, onClick = preset.onClick)
            .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.XS),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = preset.label,
            style = AstraTypography.Caption,
            color = if (preset.selected) AstraColors.TextPrimary else AstraColors.TextSecondary,
            fontWeight = if (preset.selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** 4096 → "4 096". */
private fun formatTokens(tokens: Int): String =
    tokens.toString().reversed().chunked(3).joinToString(" ").reversed()
