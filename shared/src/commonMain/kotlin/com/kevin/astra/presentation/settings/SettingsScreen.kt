package com.kevin.astra.presentation.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kevin.astra.core.design.AstraCard
import com.kevin.astra.core.design.AstraChip
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraScreen
import com.kevin.astra.core.design.AstraSpacing

@Composable
fun SettingsScreen(contentPadding: PaddingValues) {
    AstraScreen(
        title = "Settings",
        description = "Configure ASTRA's future local runtime and engineering preferences.",
        contentPadding = contentPadding,
    ) {
        AstraCard(
            title = "Runtime",
            subtitle = "Local execution is the default architecture.",
        ) {
            Spacer(Modifier.height(AstraSpacing.M))
            AstraChip("OFFLINE FIRST", color = AstraColors.Success)
        }
        AstraCard(
            title = "Appearance",
            subtitle = "ASTRA dark theme is active across Android and iOS.",
            status = "DARK",
        )
        AstraCard(
            title = "About",
            subtitle = "Secure Local AI for Critical Operations",
        )
    }
}
