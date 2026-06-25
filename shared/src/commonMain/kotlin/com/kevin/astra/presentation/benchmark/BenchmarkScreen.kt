package com.kevin.astra.presentation.benchmark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kevin.astra.core.design.AstraCard
import com.kevin.astra.core.design.AstraMetricCard
import com.kevin.astra.core.design.AstraScreen
import com.kevin.astra.core.design.AstraSpacing

@Composable
fun BenchmarkScreen(contentPadding: PaddingValues) {
    AstraScreen(
        title = "Benchmark",
        description = "Compare local models and inference backends with measurable evidence.",
        contentPadding = contentPadding,
    ) {
        AstraCard(
            title = "Benchmark lab",
            subtitle = "Execution is disabled until inference engines are introduced.",
            status = "IDLE",
        ) {
            Spacer(Modifier.height(AstraSpacing.M))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
            ) {
                AstraMetricCard("--", "ms", "Latency", Modifier.weight(1f))
                AstraMetricCard("--", "t/s", "Throughput", Modifier.weight(1f))
            }
        }
        AstraCard(
            title = "Comparison results",
            subtitle = "Model rankings and recommendations will appear here.",
        )
    }
}
