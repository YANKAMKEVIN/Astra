package com.kevin.astra.presentation.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.kevin.astra.domain.modelmanager.ModelReadinessStatus

@Composable
fun ProjectOverviewScreen(
    contentPadding: PaddingValues,
    viewModel: ProjectOverviewViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProjectOverviewContent(
        state = state,
        contentPadding = contentPadding,
        onIntent = viewModel::dispatch,
    )
}

@Composable
private fun ProjectOverviewContent(
    state: ProjectOverviewState,
    contentPadding: PaddingValues,
    onIntent: (ProjectOverviewIntent) -> Unit,
) {
    AstraScreen(
        title = "Welcome Engineer",
        description = "Your secure Edge AI operations overview.",
        contentPadding = contentPadding,
    ) {
        RuntimeOverviewCard(state = state, onRefresh = { onIntent(ProjectOverviewIntent.Refresh) })
        ArchitectureCard(items = state.architectureItems)
        ModelsCard(state = state)
        DeviceCard(state = state)
        AiFeaturesCard(features = state.aiFeatures)
        DocumentationCard(links = state.documentationLinks)
    }
}

@Composable
private fun RuntimeOverviewCard(
    state: ProjectOverviewState,
    onRefresh: () -> Unit,
) {
    AstraCard(
        title = "Runtime",
        subtitle = "Selected configuration from the local AI repositories.",
        status = state.selectedBackend?.status?.label ?: "Unknown",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraMetricCard(
                value = state.selectedBackend?.displayName ?: "Unknown",
                unit = "",
                label = "Selected Backend",
                modifier = Modifier.weight(1f),
            )
            AstraMetricCard(
                value = state.currentRuntime,
                unit = "",
                label = "Current Runtime",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(AstraSpacing.S))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraMetricCard(
                value = state.selectedBackend?.status?.label ?: "Unknown",
                unit = "",
                label = "Runtime Status",
                modifier = Modifier.weight(1f),
            )
            AstraMetricCard(
                value = state.selectedIndustry.label,
                unit = "",
                label = "Current Industry",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(AstraSpacing.S))
        Text(
            text = state.fallbackStatus,
            style = AstraTypography.Body,
            color = if (state.fallbackStatus.startsWith("Fallback active")) AstraColors.Warning else AstraColors.Success,
        )
        if (state.error != null) {
            Spacer(Modifier.height(AstraSpacing.S))
            Text(text = state.error, style = AstraTypography.Caption, color = AstraColors.Error)
        }
        Spacer(Modifier.height(AstraSpacing.M))
        AstraButton(
            text = if (state.isLoadingCapabilities) "Refreshing..." else "Refresh device overview",
            onClick = onRefresh,
            style = AstraButtonStyle.Ghost,
            enabled = !state.isLoadingCapabilities,
        )
    }
}

@Composable
private fun ArchitectureCard(items: List<OverviewArchitectureItem>) {
    AstraCard(
        title = "Architecture",
        subtitle = "Core technical building blocks used by ASTRA.",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            items.forEach { item ->
                OverviewInfoRow(title = item.title, description = item.description)
            }
        }
    }
}

@Composable
private fun ModelsCard(state: ProjectOverviewState) {
    AstraCard(
        title = "Models",
        subtitle = "Catalog metadata and local readiness are read-only here.",
        status = "${state.installedModels.size} INSTALLED",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        state.modelReadiness.forEach { readiness ->
            val catalogModel = state.availableModels.firstOrNull { it.id == readiness.modelId }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(AstraColors.SurfaceElevated, RoundedCornerShape(16.dp))
                    .border(1.dp, AstraColors.Border, RoundedCornerShape(16.dp))
                    .padding(AstraSpacing.M),
            ) {
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
                            text = "${readiness.quantization} • ${readiness.parameterCount} • ${readiness.expectedSize}",
                            style = AstraTypography.Caption,
                            color = AstraColors.TextSecondary,
                        )
                        Text(
                            text = "Context window: ${catalogModel?.contextWindow ?: 0}",
                            style = AstraTypography.Caption,
                            color = AstraColors.TextSecondary,
                        )
                    }
                    AstraChip(
                        label = readiness.status.label.uppercase(),
                        color = if (readiness.status == ModelReadinessStatus.Installed) AstraColors.Success else AstraColors.Warning,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceCard(state: ProjectOverviewState) {
    val capabilities = state.capabilities
    AstraCard(
        title = "Device",
        subtitle = "Best-effort local hardware and platform detection.",
        status = if (state.isLoadingCapabilities) "SCANNING" else if (capabilities?.npuAvailable == true) "NPU READY" else "NPU NOT DETECTED",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraMetricCard(
                value = capabilities?.platform ?: "Unknown",
                unit = "",
                label = "Platform",
                modifier = Modifier.weight(1f),
            )
            AstraMetricCard(
                value = capabilities?.osVersion ?: "Unknown",
                unit = "",
                label = "OS",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(AstraSpacing.S))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraMetricCard(
                value = capabilities?.totalMemoryMb?.let { "$it MB" } ?: "Unknown",
                unit = "",
                label = "Memory",
                modifier = Modifier.weight(1f),
            )
            AstraMetricCard(
                value = capabilities?.availableMemoryMb?.let { "$it MB" } ?: "Unknown",
                unit = "",
                label = "Available",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(AstraSpacing.S))
        DeviceInfoRow(label = "Device", value = capabilities?.deviceModel ?: "Unknown")
        DeviceInfoRow(label = "CPU", value = capabilities?.cpuName ?: "Unknown")
        DeviceInfoRow(label = "GPU", value = capabilities?.gpuName ?: "Not detected")
        DeviceInfoRow(
            label = "NPU",
            value = if (capabilities?.npuAvailable == true) capabilities.npuName else "Not detected",
        )
        DeviceInfoRow(
            label = "Storage available",
            value = capabilities?.storageAvailableGb?.let { "${((it * 10).toInt() / 10.0)} GB" } ?: "Unknown",
        )
        Spacer(Modifier.height(AstraSpacing.M))
        ChipScroller(
            title = "Supported backends",
            labels = capabilities?.supportedBackends?.map { it.label }.orEmpty(),
        )
        Spacer(Modifier.height(AstraSpacing.M))
        ChipScroller(
            title = "Supported features",
            labels = capabilities?.supportedFeatures?.map { it.label }.orEmpty(),
        )
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Column {
        Text(text = label, style = AstraTypography.Caption, color = AstraColors.TextSecondary)
        Text(text = value, style = AstraTypography.Body, color = AstraColors.TextPrimary)
    }
}

@Composable
private fun AiFeaturesCard(features: List<String>) {
    AstraCard(
        title = "AI Features",
        subtitle = "Implemented capabilities available in this build.",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        ChipScroller(title = "Capabilities", labels = features)
    }
}

@Composable
private fun DocumentationCard(links: List<OverviewDocumentationLink>) {
    AstraCard(
        title = "Documentation",
        subtitle = "Quick references for technical discussion. Paths are repository-local.",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            links.forEach { link ->
                OverviewInfoRow(
                    title = link.title,
                    description = "${link.path} — ${link.description}",
                )
            }
        }
    }
}

@Composable
private fun OverviewInfoRow(
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AstraColors.SurfaceElevated, RoundedCornerShape(16.dp))
            .border(1.dp, AstraColors.Border, RoundedCornerShape(16.dp))
            .padding(AstraSpacing.M),
    ) {
        Text(
            text = title,
            style = AstraTypography.Body,
            color = AstraColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(AstraSpacing.XS))
        Text(
            text = description,
            style = AstraTypography.Caption,
            color = AstraColors.TextSecondary,
        )
    }
}

@Composable
private fun ChipScroller(
    title: String,
    labels: List<String>,
) {
    Text(text = title, style = AstraTypography.Caption, color = AstraColors.TextSecondary)
    Spacer(Modifier.height(AstraSpacing.S))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
    ) {
        labels.ifEmpty { listOf("Unknown") }.forEach { label ->
            AstraChip(
                label = label,
                color = if (label == "Unknown") AstraColors.Warning else AstraColors.Success,
            )
        }
    }
}
