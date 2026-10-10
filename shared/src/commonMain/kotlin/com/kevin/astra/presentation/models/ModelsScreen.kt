package com.kevin.astra.presentation.models

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.design.AstraButton
import com.kevin.astra.core.design.AstraButtonStyle
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraIcon
import com.kevin.astra.core.design.AstraIcons
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import com.kevin.astra.core.device.DeviceCapabilities
import com.kevin.astra.domain.modelmanager.ModelDownloadState
import com.kevin.astra.domain.modelmanager.ModelReadinessStatus
import com.kevin.astra.domain.modelmanager.RequiredModelFile
import com.kevin.astra.presentation.settings.SettingsEffect
import com.kevin.astra.presentation.settings.SettingsIntent
import com.kevin.astra.presentation.settings.SettingsViewModel

/** Rows shown per size tier before "Show N more". */
private const val TierPreviewCount = 3

/**
 * The single place to browse, download, activate and delete on-device models.
 * Installed models come first, then the catalog grouped by RAM tier, with models
 * too large for this device folded away at the bottom.
 */
@Composable
fun ModelsScreen(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel,
    deviceCapabilities: DeviceCapabilities?,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var query by remember { mutableStateOf("") }
    var expandedRowId by remember { mutableStateOf<String?>(null) }
    var expandedTiers by remember { mutableStateOf(emptySet<ModelTier>()) }
    var showTooLarge by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is SettingsEffect.DownloadCompleted -> snackbarHostState.showSnackbar("${effect.modelName} is ready to use")
            }
        }
    }

    val sections = buildModelSections(
        readiness = state.modelReadiness,
        models = state.availableModels,
        selectedModelId = state.selectedModel?.id,
        downloadState = state.downloadState,
        deviceMemoryMb = deviceCapabilities?.totalMemoryMb,
        query = query,
    )
    val searching = query.isNotBlank()

    val rowActions = RowActions(
        onToggle = { id -> expandedRowId = if (expandedRowId == id) null else id },
        onDownload = { viewModel.dispatch(SettingsIntent.DownloadModel(it)) },
        onCancel = { viewModel.dispatch(SettingsIntent.CancelDownload(it)) },
        onUse = { viewModel.dispatch(SettingsIntent.SelectModel(it)) },
        onDelete = { viewModel.dispatch(SettingsIntent.DeleteModel(it)) },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = AstraSpacing.L)
                .padding(top = AstraSpacing.L, bottom = AstraSpacing.L),
            verticalArrangement = Arrangement.spacedBy(AstraSpacing.M),
        ) {
            Text(
                text = "Models",
                style = AstraTypography.Headline,
                color = AstraColors.TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            StorageSummary(
                usedMb = state.storageUsageMb,
                freeGb = deviceCapabilities?.storageAvailableGb?.takeIf { it > 0 },
                installedCount = state.modelReadiness.count { it.status == ModelReadinessStatus.Installed },
            )
            SearchField(query = query, onQueryChange = { query = it })

            (state.downloadState as? ModelDownloadState.Failed)?.let { failed ->
                ErrorBanner(text = failed.reason)
            }

            if (sections.isEmpty) {
                Text(
                    text = "No models match “$query”.",
                    style = AstraTypography.Body,
                    color = AstraColors.TextSecondary,
                )
            }

            if (sections.onDevice.isNotEmpty()) {
                ModelSection(title = "On this device", trailing = "${sections.onDevice.size}") {
                    sections.onDevice.forEachIndexed { i, entry ->
                        ModelRow(entry, expandedRowId == entry.id, rowActions, i < sections.onDevice.lastIndex)
                    }
                }
            }

            if (sections.downloading.isNotEmpty()) {
                ModelSection(title = "Downloading") {
                    sections.downloading.forEachIndexed { i, entry ->
                        ModelRow(entry, expandedRowId == entry.id, rowActions, i < sections.downloading.lastIndex)
                    }
                }
            }

            sections.tiers.forEach { section ->
                val showAll = searching || section.tier in expandedTiers
                val visible = if (showAll) section.entries else section.entries.take(TierPreviewCount)
                val hidden = section.entries.size - visible.size
                ModelSection(
                    title = section.tier.title,
                    subtitle = section.tier.subtitle,
                    trailing = "${section.entries.size}",
                ) {
                    visible.forEachIndexed { i, entry ->
                        ModelRow(entry, expandedRowId == entry.id, rowActions, i < visible.lastIndex)
                    }
                    if (hidden > 0) {
                        SectionFooterButton(text = "Show $hidden more") {
                            expandedTiers = expandedTiers + section.tier
                        }
                    }
                }
            }

            if (sections.tooLarge.isNotEmpty()) {
                val open = showTooLarge || searching
                CollapsibleHeader(
                    title = "Too large for this device",
                    count = sections.tooLarge.size,
                    open = open,
                    onClick = { showTooLarge = !showTooLarge },
                )
                AnimatedVisibility(visible = open) {
                    ModelSection(title = null) {
                        sections.tooLarge.forEachIndexed { i, entry ->
                            ModelRow(entry, expandedRowId == entry.id, rowActions, i < sections.tooLarge.lastIndex)
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(contentPadding),
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

private class RowActions(
    val onToggle: (String) -> Unit,
    val onDownload: (String) -> Unit,
    val onCancel: (String) -> Unit,
    val onUse: (String) -> Unit,
    val onDelete: (String) -> Unit,
)

// ── Header pieces ────────────────────────────────────────────────────────────

@Composable
private fun StorageSummary(usedMb: Float, freeGb: Double?, installedCount: Int) {
    val usedLabel = formatMb(usedMb)
    Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
        Text(
            text = buildString {
                append("$installedCount on device · $usedLabel used")
                if (freeGb != null) append(" · ${formatGb(freeGb)} free")
            },
            style = AstraTypography.Body,
            color = AstraColors.TextSecondary,
        )
        if (freeGb != null && freeGb > 0) {
            val usedGb = usedMb / 1024f
            val fraction = (usedGb / (usedGb + freeGb.toFloat())).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = AstraColors.Primary,
                trackColor = AstraColors.Border,
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AstraColors.SurfaceElevated.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(16.dp))
            .padding(horizontal = AstraSpacing.M, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
    ) {
        AstraIcon(icon = AstraIcons.Search, tint = AstraColors.TextDisabled, size = 18.dp)
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search models or providers…",
                    style = AstraTypography.Body,
                    color = AstraColors.TextDisabled,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = AstraTypography.Body.copy(color = AstraColors.TextPrimary),
                cursorBrush = SolidColor(AstraColors.Secondary),
            )
        }
        if (query.isNotEmpty()) {
            IconCircle(icon = AstraIcons.Close, tint = AstraColors.TextSecondary, onClick = { onQueryChange("") })
        }
    }
}

@Composable
private fun ErrorBanner(text: String) {
    Text(
        text = text,
        style = AstraTypography.Caption,
        color = AstraColors.Error,
        modifier = Modifier
            .fillMaxWidth()
            .background(AstraColors.Error.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .border(1.dp, AstraColors.Error.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(AstraSpacing.M),
    )
}

// ── Sections ─────────────────────────────────────────────────────────────────

@Composable
private fun ModelSection(
    title: String?,
    subtitle: String? = null,
    trailing: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
        if (title != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = AstraSpacing.S, start = AstraSpacing.XS),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = AstraTypography.Body,
                        color = AstraColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (subtitle != null) {
                        Text(text = subtitle, style = AstraTypography.Caption, color = AstraColors.TextDisabled)
                    }
                }
                if (trailing != null) {
                    Text(text = trailing, style = AstraTypography.Caption, color = AstraColors.TextDisabled)
                }
            }
        }
        // One glass panel per section, rows separated by hairlines — denser and calmer
        // than a stack of individual cards.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(AstraColors.SurfaceElevated.copy(alpha = 0.55f))
                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(18.dp)),
        ) {
            content()
        }
    }
}

@Composable
private fun SectionFooterButton(text: String, onClick: () -> Unit) {
    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
    Text(
        text = text,
        style = AstraTypography.Caption,
        color = AstraColors.Primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AstraSpacing.M, vertical = 12.dp),
    )
}

@Composable
private fun CollapsibleHeader(title: String, count: Int, open: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(top = AstraSpacing.S, bottom = AstraSpacing.XS, start = AstraSpacing.XS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
    ) {
        Text(
            text = "$title · $count",
            style = AstraTypography.Body,
            color = AstraColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        AstraIcon(
            icon = AstraIcons.ChevronDown,
            tint = AstraColors.TextSecondary,
            size = 18.dp,
            modifier = Modifier.rotate(if (open) 180f else 0f),
        )
    }
}

// ── Row ──────────────────────────────────────────────────────────────────────

@Composable
private fun ModelRow(
    entry: ModelEntry,
    expanded: Boolean,
    actions: RowActions,
    showDivider: Boolean,
) {
    val tooLarge = entry.exceedsDeviceMemory
    val downloading = entry.downloading
    val r = entry.readiness

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { actions.onToggle(entry.id) }
            .padding(horizontal = AstraSpacing.M, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.M),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
                ) {
                    Text(
                        text = r.displayName,
                        style = AstraTypography.Body,
                        color = AstraColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (entry.isActive) {
                        // A selected model that isn't installed yet falls back to Mock at runtime.
                        Text(
                            text = if (entry.isInstalled) "Active" else "Selected",
                            style = AstraTypography.Caption,
                            color = if (entry.isInstalled) AstraColors.Success else AstraColors.Warning,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Text(
                    text = when {
                        tooLarge -> "${r.expectedSize} · needs ${formatMb(entry.minimumMemoryMb.toFloat())} RAM"
                        !entry.isInstalled && !entry.isDownloadable -> "${r.expectedSize} · ${r.provider} · manual install"
                        else -> "${r.expectedSize} · ${r.provider} · ${r.parameterCount}"
                    },
                    style = AstraTypography.Caption,
                    color = AstraColors.TextDisabled,
                )
            }
            RowTrailingAction(entry = entry, tooLarge = tooLarge, actions = actions)
        }

        if (downloading != null) {
            Spacer(Modifier.height(AstraSpacing.S))
            LinearProgressIndicator(
                progress = { downloading.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = AstraColors.Primary,
                trackColor = AstraColors.Border,
                drawStopIndicator = {},
            )
            Spacer(Modifier.height(AstraSpacing.XS))
            Text(
                text = if (downloading.totalMb > 0f) {
                    "${downloading.downloadedMb.toInt()} / ${downloading.totalMb.toInt()} MB · ${downloading.progressPercent}%"
                } else {
                    "${downloading.downloadedMb.toInt()} MB downloaded…"
                },
                style = AstraTypography.Caption,
                color = AstraColors.Primary,
            )
        }

        AnimatedVisibility(visible = expanded && downloading == null) {
            ModelDetails(entry = entry, tooLarge = tooLarge, actions = actions)
        }
    }
    if (showDivider) HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
}

@Composable
private fun RowTrailingAction(entry: ModelEntry, tooLarge: Boolean, actions: RowActions) {
    when {
        entry.downloading != null ->
            IconCircle(icon = AstraIcons.Close, tint = AstraColors.Error, onClick = { actions.onCancel(entry.id) })
        entry.isInstalled && !entry.isActive ->
            PillButton(text = "Use", onClick = { actions.onUse(entry.id) })
        entry.isInstalled -> Unit
        !entry.isDownloadable ->
            AstraIcon(icon = AstraIcons.Lock, tint = AstraColors.TextDisabled, size = 18.dp)
        tooLarge -> Unit
        else ->
            IconCircle(icon = AstraIcons.Download, tint = AstraColors.Primary, onClick = { actions.onDownload(entry.id) })
    }
}

@Composable
private fun ModelDetails(entry: ModelEntry, tooLarge: Boolean, actions: RowActions) {
    val r = entry.readiness
    Column(
        modifier = Modifier.padding(top = AstraSpacing.S),
        verticalArrangement = Arrangement.spacedBy(AstraSpacing.XS),
    ) {
        Text(text = r.readinessMessage, style = AstraTypography.Caption, color = AstraColors.TextSecondary)
        DetailLine(label = "Provider", value = r.provider)
        DetailLine(label = "Parameters", value = "${r.parameterCount} · ${r.quantization}")
        if (entry.minimumMemoryMb > 0) DetailLine(label = "RAM needed", value = formatMb(entry.minimumMemoryMb.toFloat()))
        entry.model?.contextWindow?.let { DetailLine(label = "Context", value = "$it tokens") }
        DetailLine(label = "Backends", value = r.supportedBackends.joinToString { it.label })
        r.requiredFiles.forEach { RequiredFileLine(it) }

        Spacer(Modifier.height(AstraSpacing.XS))
        when {
            entry.isInstalled && r.isDownloadedToFilesDir ->
                AstraButton(
                    text = "Delete model",
                    onClick = { actions.onDelete(entry.id) },
                    style = AstraButtonStyle.Danger,
                    leadingIcon = AstraIcons.Trash,
                )
            entry.isInstalled -> Unit
            !entry.isDownloadable ->
                Text(
                    text = "Not downloadable in-app. Get it from huggingface.co/litert-community (HuggingFace account " +
                        "required) and place the file in the app's model directory.",
                    style = AstraTypography.Caption,
                    color = AstraColors.TextSecondary,
                )
            tooLarge -> {
                Text(
                    text = "This device has less RAM than this model needs — it may fail to load or be very slow.",
                    style = AstraTypography.Caption,
                    color = AstraColors.Warning,
                )
                AstraButton(
                    text = "Download anyway",
                    onClick = { actions.onDownload(entry.id) },
                    style = AstraButtonStyle.Secondary,
                    leadingIcon = AstraIcons.Download,
                )
            }
            else -> Unit // Download lives on the row itself.
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row {
        Text(
            text = label,
            style = AstraTypography.Caption,
            color = AstraColors.TextDisabled,
            modifier = Modifier.weight(0.35f),
        )
        Text(
            text = value,
            style = AstraTypography.Caption,
            color = AstraColors.TextSecondary,
            modifier = Modifier.weight(0.65f),
        )
    }
}

@Composable
private fun RequiredFileLine(file: RequiredModelFile) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = file.path.substringAfterLast('/'),
            style = AstraTypography.Caption,
            color = AstraColors.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (file.present) "Present" else "Missing",
            style = AstraTypography.Caption,
            color = if (file.present) AstraColors.Success else AstraColors.Warning,
        )
    }
}

// ── Small controls ───────────────────────────────────────────────────────────

@Composable
private fun IconCircle(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AstraIcon(icon = icon, tint = tint, size = 18.dp)
    }
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = AstraTypography.Caption,
        color = AstraColors.Primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, AstraColors.Primary.copy(alpha = 0.5f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = AstraSpacing.M, vertical = 6.dp),
    )
}

private fun formatMb(mb: Float): String =
    if (mb < 1024f) "${mb.toInt()} MB" else formatGb(mb / 1024.0)

private fun formatGb(gb: Double): String {
    val tenths = (gb * 10).toInt()
    return "${tenths / 10}.${tenths % 10} GB"
}
