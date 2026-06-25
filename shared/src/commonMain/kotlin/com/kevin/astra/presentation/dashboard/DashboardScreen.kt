package com.kevin.astra.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kevin.astra.core.design.AstraCard
import com.kevin.astra.core.design.AstraChip
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraMetricCard
import com.kevin.astra.core.design.AstraScreen
import com.kevin.astra.core.design.AstraSpacing

@Composable
fun DashboardScreen(contentPadding: PaddingValues) {
    AstraScreen(
        title = "Welcome Engineer",
        description = "Your secure Edge AI operations overview.",
        contentPadding = contentPadding,
    ) {
        AstraCard(
            title = "System status",
            subtitle = "Local environment placeholder",
            status = "READY",
        ) {
            Spacer(Modifier.height(AstraSpacing.M))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
            ) {
                AstraMetricCard("LOCAL", "", "Execution", Modifier.weight(1f))
                AstraMetricCard("0", "ms", "Latency", Modifier.weight(1f))
            }
        }
        AstraCard(
            title = "Device capabilities",
            subtitle = "Hardware discovery arrives in a future sprint.",
        ) {
            Spacer(Modifier.height(AstraSpacing.M))
            Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                AstraChip("CPU", color = AstraColors.Success)
                AstraChip("GPU", color = AstraColors.Warning)
                AstraChip("NPU", color = AstraColors.Secondary)
            }
        }
        AstraCard(
            title = "AI configuration",
            subtitle = "Model and backend selection will be connected later.",
        )
        AstraCard(
            title = "Quick actions",
            subtitle = "Use the navigation bar to explore every ASTRA workspace.",
        )
    }
}
