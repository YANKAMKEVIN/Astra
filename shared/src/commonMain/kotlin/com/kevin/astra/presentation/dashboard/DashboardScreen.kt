package com.kevin.astra.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.device.DeviceCapabilities
import com.kevin.astra.core.device.SupportedFeature
import com.kevin.astra.core.design.AstraCard
import com.kevin.astra.core.design.AstraChip
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraMetricCard
import com.kevin.astra.core.design.AstraScreen
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography

@Composable
fun DashboardScreen(
    contentPadding: PaddingValues,
    viewModel: DashboardViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DashboardContent(
        state = state,
        contentPadding = contentPadding,
    )
}

@Composable
private fun DashboardContent(
    state: DashboardState,
    contentPadding: PaddingValues,
) {
    val capabilities = state.capabilities

    AstraScreen(
        title = "Welcome Engineer",
        description = "Your secure Edge AI operations overview.",
        contentPadding = contentPadding,
    ) {
        AstraCard(
            title = "System status",
            subtitle = if (state.isLoadingCapabilities) {
                "Reading local device capabilities..."
            } else {
                "Local environment detected through DeviceCapabilityProvider."
            },
            status = if (state.isLoadingCapabilities) "SCANNING" else "READY",
        ) {
            Spacer(Modifier.height(AstraSpacing.M))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
            ) {
                AstraMetricCard(
                    value = capabilities?.platform ?: "Unknown",
                    unit = "",
                    label = "Platform",
                    modifier = Modifier.weight(1f),
                )
                AstraMetricCard(
                    value = if (capabilities?.supportedFeatures?.contains(SupportedFeature.OfflineMode) == true) "ON" else "—",
                    unit = "",
                    label = "Offline",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        DeviceCapabilitiesCard(capabilities = capabilities)

        AstraCard(
            title = "AI configuration",
            subtitle = capabilities?.supportedBackends?.joinToString { it.label }
                ?: "Supported backend detection pending.",
        )
        AstraCard(
            title = "Quick actions",
            subtitle = "Use the navigation bar to explore every ASTRA workspace.",
        )
    }
}

@Composable
private fun DeviceCapabilitiesCard(capabilities: DeviceCapabilities?) {
    AstraCard(
        title = "Device capabilities",
        subtitle = "Best-effort local hardware and platform detection.",
        status = if (capabilities?.npuAvailable == true) "NPU READY" else "NPU NOT DETECTED",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraMetricCard(
                value = capabilities?.osVersion ?: "Unknown",
                unit = "",
                label = "OS",
                modifier = Modifier.weight(1f),
            )
            AstraMetricCard(
                value = capabilities?.deviceModel ?: "Unknown",
                unit = "",
                label = "Device",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(AstraSpacing.S))
        Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            AstraMetricCard(
                value = capabilities?.totalMemoryMb?.formatMemory() ?: "Unknown",
                unit = "",
                label = "Memory",
                modifier = Modifier.weight(1f),
            )
            AstraMetricCard(
                value = capabilities?.availableMemoryMb?.formatMemory() ?: "Unknown",
                unit = "",
                label = "Available",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(AstraSpacing.S))
        CapabilityDetail(label = "CPU", value = capabilities?.cpuName ?: "Unknown")
        CapabilityDetail(label = "GPU", value = capabilities?.gpuName ?: "Not detected")
        CapabilityDetail(
            label = "NPU",
            value = if (capabilities?.npuAvailable == true) capabilities.npuName else "Not detected",
        )
        CapabilityDetail(
            label = "Storage available",
            value = capabilities?.storageAvailableGb?.formatStorage() ?: "Unknown",
        )
        Spacer(Modifier.height(AstraSpacing.M))
        CapabilityChipSection(
            title = "Supported backends",
            labels = capabilities?.supportedBackends?.map { it.label }.orEmpty(),
        )
        Spacer(Modifier.height(AstraSpacing.M))
        CapabilityChipSection(
            title = "Supported features",
            labels = capabilities?.supportedFeatures?.map { it.label }.orEmpty(),
        )
    }
}

@Composable
private fun CapabilityDetail(
    label: String,
    value: String,
) {
    Column {
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
}

@Composable
private fun CapabilityChipSection(
    title: String,
    labels: List<String>,
) {
    Text(
        text = title,
        style = AstraTypography.Caption,
        color = AstraColors.TextSecondary,
    )
    Spacer(Modifier.height(AstraSpacing.S))
    Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
        val chips = labels.ifEmpty { listOf("Unknown") }
        chips.forEach { label ->
            AstraChip(
                label = label,
                color = if (label == "Unknown") AstraColors.Warning else AstraColors.Success,
            )
        }
    }
}

private fun Long.formatMemory(): String =
    if (this > 0) "$this MB" else "Unknown"

private fun Double.formatStorage(): String =
    if (this > 0.0) "${((this * 10).toInt() / 10.0)} GB" else "Unknown"
