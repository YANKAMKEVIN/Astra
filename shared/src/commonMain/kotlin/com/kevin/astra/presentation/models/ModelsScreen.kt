package com.kevin.astra.presentation.models

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.design.AstraChip
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraIcon
import com.kevin.astra.core.design.AstraIcons
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import com.kevin.astra.domain.modelmanager.ModelReadiness
import com.kevin.astra.domain.modelmanager.ModelReadinessStatus
import com.kevin.astra.presentation.overview.ProjectOverviewViewModel

private enum class ModelFilter(val label: String) {
    All("All"),
    Installed("Installed"),
    Available("Available"),
    Missing("Missing"),
}

@Composable
fun ModelsScreen(
    contentPadding: PaddingValues,
    viewModel: ProjectOverviewViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ModelFilter.All) }

    val models = state.modelReadiness
    val installedCount = models.count { it.status == ModelReadinessStatus.Installed }
    val filtered = models
        .filter { m ->
            when (filter) {
                ModelFilter.All -> true
                ModelFilter.Installed -> m.status == ModelReadinessStatus.Installed
                ModelFilter.Available -> m.status == ModelReadinessStatus.ModelRequired
                ModelFilter.Missing -> m.status == ModelReadinessStatus.MissingFiles
            }
        }
        .filter { m ->
            query.isBlank() ||
                m.displayName.contains(query, ignoreCase = true) ||
                m.provider.contains(query, ignoreCase = true)
        }

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
        Text(
            text = "$installedCount installed · On-device",
            style = AstraTypography.Body,
            color = AstraColors.TextSecondary,
        )

        // Search
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
                        text = "Search models…",
                        style = AstraTypography.Body,
                        color = AstraColors.TextDisabled,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = AstraTypography.Body.copy(color = AstraColors.TextPrimary),
                    cursorBrush = SolidColor(AstraColors.Secondary),
                )
            }
        }

        // Filters
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        ) {
            ModelFilter.entries.forEach { f ->
                FilterChip(label = f.label, selected = f == filter, onClick = { filter = f })
            }
        }

        Spacer(Modifier.height(AstraSpacing.XS))

        if (filtered.isEmpty()) {
            Text(
                text = "No models match this filter.",
                style = AstraTypography.Body,
                color = AstraColors.TextSecondary,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                filtered.forEach { model -> ModelRow(model = model) }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) AstraColors.Primary else AstraColors.SurfaceElevated.copy(alpha = 0.55f),
            )
            .border(
                1.dp,
                if (selected) Color.Transparent else Color.White.copy(alpha = 0.07f),
                RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
    ) {
        Text(
            text = label,
            style = AstraTypography.Caption,
            color = if (selected) Color.White else AstraColors.TextSecondary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ModelRow(model: ModelReadiness) {
    val statusColor = when (model.status) {
        ModelReadinessStatus.Installed -> AstraColors.Success
        ModelReadinessStatus.ModelRequired -> AstraColors.Warning
        ModelReadinessStatus.MissingFiles -> AstraColors.Warning
        else -> AstraColors.TextDisabled
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AstraColors.SurfaceElevated.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
            .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.M),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AstraSpacing.M),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(statusColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            AstraIcon(icon = AstraIcons.Cube, tint = statusColor, size = 20.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.displayName,
                style = AstraTypography.Body,
                color = AstraColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${model.parameterCount} · ${model.quantization} · ${model.expectedSize}",
                style = AstraTypography.Caption,
                color = AstraColors.TextDisabled,
            )
        }
        AstraChip(label = model.status.label.uppercase(), color = statusColor)
    }
}
