package com.kevin.astra.presentation.overview

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.design.AstraChip
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraCore
import com.kevin.astra.core.design.AstraIcon
import com.kevin.astra.core.design.AstraIcons
import com.kevin.astra.core.design.AstraScreen
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import com.kevin.astra.core.navigation.AstraDestination
import com.kevin.astra.domain.modelmanager.ModelReadinessStatus
import com.kevin.astra.domain.settings.DemoModeHolder

@Composable
fun ProjectOverviewScreen(
    contentPadding: PaddingValues,
    viewModel: ProjectOverviewViewModel,
    onNavigate: (AstraDestination) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isDemoMode by DemoModeHolder.enabled.collectAsStateWithLifecycle()

    // The overview reads device capabilities only at init, and the view model is
    // a shared instance — so a failed/empty initial read would otherwise be
    // unrecoverable without an app restart. Retry on resume when it's missing.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val current = viewModel.state.value
        if (!current.isLoadingCapabilities &&
            (current.capabilities == null || current.error != null)
        ) {
            viewModel.dispatch(ProjectOverviewIntent.Refresh)
        }
    }

    AstraScreen(
        title = "Overview",
        description = "Local AI status, models, and device readiness.",
        contentPadding = contentPadding,
        showDemoIndicator = false,
    ) {
        StatusHeader(state = state, isDemoMode = isDemoMode)
        PrivateComputeStrip()
        LiveMetricsGrid(state = state)
        ModelsCard(state = state, onSeeAll = { onNavigate(AstraDestination.Models) })
        AiFeaturesSection(features = state.aiFeatures)
        if (!state.isLoadingCapabilities) {
            DeviceDetailSection(state = state)
        }
    }
}

// ── Status header ─────────────────────────────────────────────────────────────

@Composable
private fun StatusHeader(state: ProjectOverviewState, isDemoMode: Boolean) {
    val infinite = rememberInfiniteTransition(label = "status-dot")
    val dotAlpha by infinite.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "dot",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AstraColors.SurfaceElevated.copy(alpha = 0.72f))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.035f), Color.Transparent),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(AstraSpacing.M),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AstraSpacing.M),
            ) {
                AstraCore(coreSize = 52.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "LOCAL AI",
                        style = AstraTypography.Caption.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.4.sp,
                        ),
                        color = AstraColors.Secondary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Ready",
                        style = AstraTypography.Title.copy(
                            fontSize = 24.sp,
                            lineHeight = 30.sp,
                        ),
                        color = AstraColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(
                    modifier = Modifier
                        .background(
                            if (isDemoMode) AstraColors.Warning.copy(alpha = 0.12f)
                            else AstraColors.Success.copy(alpha = 0.12f),
                            RoundedCornerShape(50),
                        )
                        .border(
                            1.dp,
                            if (isDemoMode) AstraColors.Warning.copy(alpha = 0.35f)
                            else AstraColors.Success.copy(alpha = 0.35f),
                            RoundedCornerShape(50),
                        )
                        .padding(horizontal = AstraSpacing.S, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AstraSpacing.XS),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .alpha(dotAlpha)
                            .background(
                                if (isDemoMode) AstraColors.Warning else AstraColors.Success,
                                CircleShape,
                            ),
                    )
                    Text(
                        text = if (isDemoMode) "DEMO" else "LIVE",
                        style = AstraTypography.Caption.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                        ),
                        color = if (isDemoMode) AstraColors.Warning else AstraColors.Success,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Key info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
            ) {
                StatPill(
                    label = "Model",
                    value = state.selectedModel?.displayName ?: "None",
                    modifier = Modifier.weight(1f),
                )
                StatPill(
                    label = "Backend",
                    value = state.selectedBackend?.displayName ?: "—",
                    modifier = Modifier.weight(1f),
                )
                StatPill(
                    label = "Domain",
                    value = state.selectedIndustry?.label?.split(" ")?.first() ?: "None",
                    modifier = Modifier.weight(1f),
                )
            }

            // Runtime status line
            if (state.fallbackStatus.startsWith("Fallback")) {
                Row(
                    modifier = Modifier
                        .background(AstraColors.Warning.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                        .border(1.dp, AstraColors.Warning.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                        .padding(horizontal = AstraSpacing.S, vertical = AstraSpacing.XS),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AstraSpacing.XS),
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(AstraColors.Warning, CircleShape),
                    )
                    Text(
                        text = formatFallbackStatus(state.fallbackStatus),
                        style = AstraTypography.Caption,
                        color = AstraColors.Warning,
                    )
                }
            }

            if (state.error != null) {
                Text(text = "⚠ ${state.error}", style = AstraTypography.Caption, color = AstraColors.Error)
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(AstraColors.SurfaceElevated, RoundedCornerShape(12.dp))
            .border(1.dp, AstraColors.Border, RoundedCornerShape(12.dp))
            .padding(horizontal = AstraSpacing.S, vertical = AstraSpacing.S),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = label, style = AstraTypography.Caption, color = AstraColors.TextSecondary)
        Text(
            text = value,
            style = AstraTypography.Caption,
            color = AstraColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

// ── Private compute strip ─────────────────────────────────────────────────────

@Composable
private fun PrivateComputeStrip() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AstraColors.Secondary.copy(alpha = 0.06f))
            .border(1.dp, AstraColors.Secondary.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
            .padding(AstraSpacing.L),
        verticalArrangement = Arrangement.spacedBy(AstraSpacing.M),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        ) {
            AstraIcon(icon = AstraIcons.Shield, tint = AstraColors.Secondary, size = 18.dp)
            Text(
                text = "PRIVATE COMPUTE",
                style = AstraTypography.Caption.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                ),
                color = AstraColors.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = "Prompts, files, and responses stay local by default.",
            style = AstraTypography.Caption,
            color = AstraColors.TextPrimary.copy(alpha = 0.72f),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        ) {
            ComputeStat("Network", "OFF", AstraColors.Success, Modifier.weight(1f))
            ComputeStat("Cloud", "NONE", AstraColors.Success, Modifier.weight(1f))
            ComputeStat("Inference", "LOCAL", AstraColors.Secondary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ComputeStat(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = AstraTypography.Caption, color = AstraColors.TextSecondary)
        Text(
            text = value,
            style = AstraTypography.Caption.copy(fontFamily = FontFamily.Monospace),
            color = valueColor,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── Live metrics grid ─────────────────────────────────────────────────────────

@Composable
private fun LiveMetricsGrid(state: ProjectOverviewState) {
    val caps = state.capabilities
    SectionLabel("Device")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
    ) {
        MetricTile(
            icon = AstraIcons.Memory,
            label = "RAM avail.",
            value = caps?.availableMemoryMb?.let { "$it MB" } ?: "—",
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            icon = AstraIcons.Smartphone,
            label = "Platform",
            value = caps?.platform ?: "—",
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            icon = AstraIcons.Cpu,
            label = "NPU",
            value = if (caps?.npuAvailable == true) "Ready" else "None",
            tint = if (caps?.npuAvailable == true) AstraColors.Success else AstraColors.Secondary,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(AstraSpacing.S))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
    ) {
        MetricTile(
            icon = AstraIcons.Layers,
            label = "Storage",
            value = caps?.storageAvailableGb?.let { "${(it * 10).toLong() / 10.0} GB" } ?: "—",
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            icon = AstraIcons.Memory,
            label = "RAM total",
            value = caps?.totalMemoryMb?.let { "$it MB" } ?: "—",
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            icon = AstraIcons.Terminal,
            label = "Runtime",
            value = state.currentRuntime,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tint: Color = AstraColors.Secondary,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(AstraColors.SurfaceElevated.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
            .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
        verticalArrangement = Arrangement.spacedBy(AstraSpacing.XS),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            AstraIcon(icon = icon, tint = tint, size = 16.dp)
        }
        Text(text = value, style = AstraTypography.Caption, color = AstraColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(text = label, style = AstraTypography.Caption, color = AstraColors.TextSecondary)
    }
}

// ── Models card ───────────────────────────────────────────────────────────────

@Composable
private fun ModelsCard(state: ProjectOverviewState, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = AstraSpacing.XS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "MODELS  ·  ${state.installedModels.size} INSTALLED",
            style = AstraTypography.Caption.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp,
            ),
            color = AstraColors.TextSecondary,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onSeeAll)
                .padding(horizontal = AstraSpacing.S, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "See all",
                style = AstraTypography.Caption,
                color = AstraColors.Secondary,
                fontWeight = FontWeight.SemiBold,
            )
            AstraIcon(icon = AstraIcons.ChevronRight, tint = AstraColors.Secondary, size = 14.dp)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
        // Home shows only the installed models; the full catalog lives on the
        // dedicated Models screen (via "See all").
        state.modelReadiness.filter { it.status == ModelReadinessStatus.Installed }.forEach { readiness ->
            val isInstalled = readiness.status == ModelReadinessStatus.Installed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AstraColors.Surface, RoundedCornerShape(14.dp))
                    .border(1.dp, AstraColors.Border, RoundedCornerShape(14.dp))
                    .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.M),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AstraSpacing.M),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isInstalled) AstraColors.Success else AstraColors.TextDisabled,
                            CircleShape,
                        ),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = readiness.displayName,
                        style = AstraTypography.Caption,
                        color = AstraColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${readiness.parameterCount} · ${readiness.quantization} · ${readiness.expectedSize}",
                        style = AstraTypography.Caption,
                        color = AstraColors.TextSecondary,
                    )
                }
                AstraChip(
                    label = readiness.status.label.uppercase(),
                    color = if (isInstalled) AstraColors.Success else AstraColors.Warning,
                )
            }
        }
    }
}

// ── AI Features ───────────────────────────────────────────────────────────────

@Composable
private fun AiFeaturesSection(features: List<String>) {
    if (features.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val visibleFeatures = if (expanded) features else features.take(3)
    val remainingCount = features.size - visibleFeatures.size

    SectionLabel("Capabilities")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
    ) {
        visibleFeatures.forEach { feature ->
            AstraChip(label = feature, color = AstraColors.Primary)
        }
        if (remainingCount > 0) {
            // Keep the compact look, but let the "+N" reveal the rest so no
            // capability is permanently hidden.
            AstraChip(
                label = "+$remainingCount",
                color = AstraColors.Secondary,
                modifier = Modifier.clickable { expanded = true },
            )
        }
    }
}

// ── Device detail ─────────────────────────────────────────────────────────────

@Composable
private fun DeviceDetailSection(state: ProjectOverviewState) {
    val caps = state.capabilities ?: return
    SectionLabel("Hardware")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AstraColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, AstraColors.Border, RoundedCornerShape(16.dp))
            .padding(AstraSpacing.M),
        verticalArrangement = Arrangement.spacedBy(AstraSpacing.S),
    ) {
        InfoRow(AstraIcons.Smartphone, "Device", caps.deviceModel)
        InfoRow(AstraIcons.Cpu, "CPU", caps.cpuName)
        InfoRow(AstraIcons.Monitor, "GPU", caps.gpuName ?: "Not detected")
        InfoRow(AstraIcons.Cpu, "NPU", if (caps.npuAvailable) caps.npuName else "Not detected")
        InfoRow(AstraIcons.Layers, "OS", formatOperatingSystem(caps.platform, caps.osVersion))
    }
    if (caps.supportedBackends.isNotEmpty()) {
        Spacer(Modifier.height(AstraSpacing.S))
        SectionLabel("Supported backends")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        ) {
            caps.supportedBackends.forEach { b ->
                AstraChip(label = b.label, color = AstraColors.Secondary)
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(AstraColors.Secondary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                AstraIcon(icon = icon, tint = AstraColors.Secondary, size = 16.dp)
            }
            Text(text = label, style = AstraTypography.Caption, color = AstraColors.TextSecondary)
        }
        Spacer(Modifier.width(AstraSpacing.M))
        Text(
            text = value,
            style = AstraTypography.Caption,
            color = AstraColors.TextPrimary,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = AstraTypography.Caption.copy(
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp,
        ),
        color = AstraColors.TextSecondary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = AstraSpacing.XS),
    )
}

private fun formatOperatingSystem(platform: String, osVersion: String): String {
    val readableVersion = osVersion
        .replace(" (API ", " · API ")
        .removeSuffix(")")

    return if (readableVersion.startsWith(platform)) {
        readableVersion
    } else {
        "$platform $readableVersion"
    }
}

private fun formatFallbackStatus(status: String): String =
    status.replace("No fallback required for selected ", "No fallback · ")
