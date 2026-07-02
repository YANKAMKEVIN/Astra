package com.kevin.astra.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun AstraScreen(
    title: String,
    description: String? = null,
    contentPadding: PaddingValues,
    showDemoIndicator: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(AstraSpacing.L),
        verticalArrangement = Arrangement.spacedBy(AstraSpacing.M),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = AstraTypography.Headline,
                color = AstraColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (showDemoIndicator) {
                AstraDemoModeIndicator()
            }
        }
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = AstraTypography.Body,
                color = AstraColors.TextSecondary,
            )
        }
        content()
    }
}
