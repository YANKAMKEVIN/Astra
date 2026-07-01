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
import com.kevin.astra.core.design.AstraMetricCard
import com.kevin.astra.core.design.AstraScreen
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kevin.astra.domain.modelmanager.ModelReadiness
import com.kevin.astra.domain.modelmanager.ModelReadinessStatus
import com.kevin.astra.domain.modelmanager.RequiredModelFile

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
        ModelManagerCard(
            modelReadiness = state.modelReadiness,
            models = state.availableModels,
            downloadState = state.downloadState,
            storageUsageMb = state.storageUsageMb,
            onDownload = { onIntent(SettingsIntent.DownloadModel(it)) },
            onDelete = { onIntent(SettingsIntent.DeleteModel(it)) },
            onCancel = { onIntent(SettingsIntent.CancelDownload(it)) },
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
        LightThemeCard(
            enabled = state.lightThemeEnabled,
            onToggle = { onIntent(SettingsIntent.ToggleLightTheme(it)) },
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
                val installed = model.status != ModelStatus.Unsupported
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
private fun ModelManagerCard(
    modelReadiness: List<ModelReadiness>,
    models: List<LocalModel>,
    downloadState: ModelDownloadState,
    storageUsageMb: Float,
    onDownload: (String) -> Unit,
    onDelete: (String) -> Unit,
    onCancel: (String) -> Unit,
) {
    val readyCount = modelReadiness.count { it.status == ModelReadinessStatus.Installed }
    val storageLabel = if (storageUsageMb < 1024f) {
        "${storageUsageMb.toInt()} MB used"
    } else {
        val gb = storageUsageMb / 1024f
        "${(gb * 10).toInt() / 10}.${(gb * 10).toInt() % 10} GB used"
    }
    AstraCard(
        title = "Model Manager",
        subtitle = "Download and manage on-device AI models. $storageLabel.",
        status = "$readyCount/${modelReadiness.size} READY",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            modelReadiness.forEach { readiness ->
                val model = models.firstOrNull { it.id == readiness.modelId }
                ModelReadinessRow(
                    readiness = readiness,
                    model = model,
                    downloadState = downloadState,
                    onDownload = onDownload,
                    onDelete = onDelete,
                    onCancel = onCancel,
                )
            }
        }
    }
}

@Composable
private fun ModelReadinessRow(
    readiness: ModelReadiness,
    model: LocalModel?,
    downloadState: ModelDownloadState,
    onDownload: (String) -> Unit,
    onDelete: (String) -> Unit,
    onCancel: (String) -> Unit,
) {
    val isThisDownloading = downloadState is ModelDownloadState.Downloading &&
        downloadState.modelId == readiness.modelId
    val downloading = downloadState as? ModelDownloadState.Downloading
    val isInstalled = readiness.status == ModelReadinessStatus.Installed
    var expanded by remember { mutableStateOf(isThisDownloading) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AstraColors.SurfaceElevated, RoundedCornerShape(18.dp))
            .border(
                1.dp,
                if (isInstalled) AstraColors.Success.copy(alpha = 0.4f) else AstraColors.Border,
                RoundedCornerShape(18.dp),
            )
            .clickable(enabled = !isThisDownloading) { expanded = !expanded }
            .padding(AstraSpacing.M),
    ) {
        // ── Compact header (always visible) ──────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = readiness.displayName,
                    style = AstraTypography.Body,
                    color = AstraColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${readiness.parameterCount} • ${readiness.quantization} • ${readiness.expectedSize}",
                    style = AstraTypography.Caption,
                    color = AstraColors.TextSecondary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S), verticalAlignment = Alignment.CenterVertically) {
                AstraChip(
                    label = if (isThisDownloading) "DOWNLOADING" else readiness.status.label.uppercase(),
                    color = if (isThisDownloading) AstraColors.Primary else readiness.status.statusColor(),
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = AstraTypography.Caption,
                    color = AstraColors.TextSecondary,
                )
            }
        }

        // ── Downloading progress (always visible when active) ─────────────
        if (isThisDownloading && downloading != null) {
            Spacer(Modifier.height(AstraSpacing.S))
            LinearProgressIndicator(
                progress = { downloading.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = AstraColors.Primary,
                trackColor = AstraColors.Border,
            )
            Spacer(Modifier.height(AstraSpacing.XS))
            Text(
                text = if (downloading.totalMb > 0f) {
                    "${downloading.downloadedMb.toInt()} / ${downloading.totalMb.toInt()} MB — ${downloading.progressPercent}%"
                } else {
                    "${downloading.downloadedMb.toInt()} MB downloaded…"
                },
                style = AstraTypography.Caption,
                color = AstraColors.Primary,
            )
            Spacer(Modifier.height(AstraSpacing.S))
            AstraButton(
                text = "Cancel",
                onClick = { onCancel(readiness.modelId) },
                style = AstraButtonStyle.Danger,
            )
        }

        // ── Expanded details ──────────────────────────────────────────────
        AnimatedVisibility(visible = expanded && !isThisDownloading) {
            Column {
                Spacer(Modifier.height(AstraSpacing.S))
                Text(
                    text = readiness.readinessMessage,
                    style = AstraTypography.Caption,
                    color = AstraColors.TextSecondary,
                )
                Spacer(Modifier.height(AstraSpacing.S))
                MetadataLine(label = "Provider", value = readiness.provider)
                MetadataLine(label = "Backends", value = readiness.supportedBackends.joinToString { it.label })
                if (readiness.requiredFiles.isNotEmpty()) {
                    Spacer(Modifier.height(AstraSpacing.S))
                    Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.XS)) {
                        readiness.requiredFiles.forEach { file -> RequiredFileRow(file = file) }
                    }
                }
                Spacer(Modifier.height(AstraSpacing.S))
                when {
                    isInstalled && model?.status != ModelStatus.Installed -> {
                        AstraButton(
                            text = "Delete model",
                            onClick = { onDelete(readiness.modelId) },
                            style = AstraButtonStyle.Danger,
                        )
                    }
                    model?.downloadUrl != null && !isInstalled -> {
                        AstraButton(
                            text = "Download model",
                            onClick = { onDownload(readiness.modelId) },
                            style = AstraButtonStyle.Primary,
                        )
                    }
                    else -> {
                        AstraButton(
                            text = "Installed",
                            onClick = {},
                            style = AstraButtonStyle.Secondary,
                            enabled = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataLine(
    label: String,
    value: String,
) {
    Text(
        text = "$label: $value",
        style = AstraTypography.Caption,
        color = AstraColors.TextSecondary,
    )
}

@Composable
private fun RequiredFileRow(file: RequiredModelFile) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.path,
                style = AstraTypography.Caption,
                color = AstraColors.TextPrimary,
            )
            Text(
                text = file.description,
                style = AstraTypography.Caption,
                color = AstraColors.TextSecondary,
            )
        }
        AstraChip(
            label = if (file.present) "PRESENT" else "MISSING",
            color = if (file.present) AstraColors.Success else AstraColors.Warning,
        )
    }
}

private fun ModelReadinessStatus.statusColor(): Color =
    when (this) {
        ModelReadinessStatus.Installed -> AstraColors.Success
        ModelReadinessStatus.ModelRequired -> AstraColors.Secondary
        ModelReadinessStatus.MissingFiles -> AstraColors.Warning
        ModelReadinessStatus.UnsupportedPlatform -> AstraColors.Error
        ModelReadinessStatus.ComingSoon -> AstraColors.TextSecondary
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
        ParameterStepper(
            label = "Temperature",
            description = "Controls randomness of the output. Low = focused, high = creative.",
            value = state.temperature.toString(),
            rangeLabel = "0.0 – 2.0",
            canDecrease = state.temperature > 0.0,
            canIncrease = state.temperature < 2.0,
            onDecrease = {
                onIntent(SettingsIntent.UpdateTemperature(roundParameter(state.temperature - 0.1)))
            },
            onIncrease = {
                onIntent(SettingsIntent.UpdateTemperature(roundParameter(state.temperature + 0.1)))
            },
        )
        ParameterStepper(
            label = "Max tokens",
            description = "Maximum number of tokens the model will generate per response.",
            value = state.maxTokens.toString(),
            rangeLabel = "128 – 4 096",
            canDecrease = state.maxTokens > 128,
            canIncrease = state.maxTokens < 4_096,
            onDecrease = {
                onIntent(SettingsIntent.UpdateMaxTokens(state.maxTokens - 128))
            },
            onIncrease = {
                onIntent(SettingsIntent.UpdateMaxTokens(state.maxTokens + 128))
            },
        )
        ParameterStepper(
            label = "Context window",
            description = "Total tokens (prompt + response) the model can process at once.",
            value = state.contextWindow.toString(),
            rangeLabel = "1 024 – 32 768",
            canDecrease = state.contextWindow > 1_024,
            canIncrease = state.contextWindow < 32_768,
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
private fun LightThemeCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    AstraCard(
        title = "Light Theme",
        subtitle = "Switch between dark and light color palettes. Dark mode is the default for edge environments.",
        status = if (enabled) "LIGHT" else "DARK",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AstraButton(
                text = if (enabled) "Switch to dark" else "Switch to light",
                onClick = { onToggle(!enabled) },
                modifier = Modifier.weight(1f),
                style = AstraButtonStyle.Secondary,
            )
            AstraChip(
                label = if (enabled) "☀" else "🌙",
                color = if (enabled) AstraColors.Warning else AstraColors.Primary,
            )
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
    description: String,
    value: String,
    rangeLabel: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AstraSpacing.XS),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = AstraTypography.Body, color = AstraColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(text = description, style = AstraTypography.Caption, color = AstraColors.TextSecondary)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiniControlButton(text = "−", enabled = canDecrease, onClick = onDecrease)
                Text(text = value, style = AstraTypography.Body, color = AstraColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                MiniControlButton(text = "+", enabled = canIncrease, onClick = onIncrease)
            }
        }
        Spacer(Modifier.height(AstraSpacing.XS))
        Text(text = "Range: $rangeLabel", style = AstraTypography.Caption, color = AstraColors.TextDisabled)
        Spacer(Modifier.height(AstraSpacing.S))
    }
}

@Composable
private fun MiniControlButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .background(AstraColors.SurfaceElevated, RoundedCornerShape(12.dp))
            .border(1.dp, if (enabled) AstraColors.Border else AstraColors.TextDisabled, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AstraTypography.Title,
            color = if (enabled) AstraColors.TextPrimary else AstraColors.TextDisabled,
        )
    }
}

private fun roundParameter(value: Double): Double =
    (value * 10).toInt() / 10.0
