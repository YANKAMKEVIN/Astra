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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.design.AstraButton
import com.kevin.astra.core.design.AstraButtonStyle
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

    AstraScreen(
        title = "ASTRA Dashboard",
        description = null,
        contentPadding = contentPadding,
    ) {
        StatusHeader(state = state, isDemoMode = isDemoMode)
        PrivateComputeStrip()
        LiveMetricsGrid(state = state)
        ModelsCard(state = state, onSeeAll = { onNavigate(AstraDestination.Models) })
        AiFeaturesSection(features = state.aiFeatures)
        if (!state.isLoadingCapabilities) {
            DeviceDetailSection(state = state)
        }
        AstraButton(
            text = if (state.isLoadingCapabilities) "Scanning device…" else "↺  Refresh",
            onClick = { viewModel.dispatch(ProjectOverviewIntent.Refresh) },
            style = AstraButtonStyle.Ghost,
            enabled = !state.isLoadingCapabilities,
            modifier = Modifier.fillMaxWidth(),
        )
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
            .clip(RoundedCornerShape(24.dp))
            .background(AstraColors.SurfaceElevated.copy(alpha = 0.6f))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(24.dp))
            .padding(AstraSpacing.L),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.M)) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
                ) {
                    AstraCore(coreSize = 34.dp)
                    Column {
                        Text(
                            text = "EDGE AI CORE",
                            style = AstraTypography.Caption.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp,
                            ),
                            color = AstraColors.Secondary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(AstraSpacing.XS))
                        Text(
                            text = "ASTRA",
                            style = AstraTypography.Headline,
                            color = AstraColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                // Live/Demo badge
                Row(
                    modifier = Modifier
                        .background(
                            if (isDemoMode) AstraColors.Warning.copy(alpha = 0.12f)
                            else AstraColors.Success.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp),
                        )
                        .border(
                            1.dp,
                            if (isDemoMode) AstraColors.Warning.copy(alpha = 0.35f)
                            else AstraColors.Success.copy(alpha = 0.35f),
                            RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
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
                        style = AstraTypography.Caption.copy(fontFamily = FontFamily.Monospace),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AstraSpacing.XS),
            ) {
                val isFallback = state.fallbackStatus.startsWith("Fallback")
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(
                            if (isFallback) AstraColors.Warning else AstraColors.Primary,
                            CircleShape,
                        ),
                )
                Text(
                    text = state.fallbackStatus,
                    style = AstraTypography.Caption,
                    color = if (isFallback) AstraColors.Warning else AstraColors.TextSecondary,
                )
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
        Text(text = label, style = AstraTypography.Caption, color = AstraColors.TextDisabled)
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
            .clip(RoundedCornerShape(20.dp))
            .background(AstraColors.Secondary.copy(alpha = 0.06f))
            .border(1.dp, AstraColors.Secondary.copy(alpha = 0.20f), RoundedCornerShape(20.dp))
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
            text = "Your data never leaves this device.",
            style = AstraTypography.Caption,
            color = AstraColors.TextSecondary,
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
        Text(text = label, style = AstraTypography.Caption, color = AstraColors.TextDisabled)
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
            .padding(AstraSpacing.M),
        verticalArrangement = Arrangement.spacedBy(AstraSpacing.S),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            AstraIcon(icon = icon, tint = tint, size = 18.dp)
        }
        Text(text = value, style = AstraTypography.Caption, color = AstraColors.TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(text = label, style = AstraTypography.Caption, color = AstraColors.TextDisabled)
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
            color = AstraColors.TextDisabled,
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
        state.modelReadiness.forEach { readiness ->
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
                        color = AstraColors.TextDisabled,
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
    SectionLabel("Capabilities")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
    ) {
        features.forEach { feature ->
            AstraChip(label = feature, color = AstraColors.Primary)
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
        InfoRow("Device", caps.deviceModel)
        InfoRow("CPU", caps.cpuName)
        InfoRow("GPU", caps.gpuName ?: "Not detected")
        InfoRow("NPU", if (caps.npuAvailable) caps.npuName else "Not detected")
        InfoRow("OS", "${caps.platform} ${caps.osVersion}")
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
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = AstraTypography.Caption, color = AstraColors.TextDisabled)
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
        color = AstraColors.TextDisabled,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = AstraSpacing.XS),
    )
}
