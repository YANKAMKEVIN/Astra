package com.kevin.astra.presentation.benchmark

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevin.astra.core.design.AstraCard
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import com.kevin.astra.domain.benchmark.BenchmarkResult

@Composable
fun BenchmarkChartCard(results: List<BenchmarkResult>) {
    if (results.isEmpty()) return

    AstraCard(
        title = "Performance Chart",
        subtitle = "Tokens/s and Task Score comparison across tested models.",
        status = "${results.size} MODELS",
    ) {
        Spacer(Modifier.height(AstraSpacing.M))
        Text("Tokens / second", style = AstraTypography.Caption, color = AstraColors.TextSecondary)
        Spacer(Modifier.height(AstraSpacing.S))
        HorizontalBarChart(
            entries = results.map { r ->
                BarEntry(
                    label = r.model.displayName,
                    value = r.tokensPerSecond?.toFloat() ?: 0f,
                    color = AstraColors.Secondary,
                )
            },
            modifier = Modifier.fillMaxWidth().height((results.size * 48 + 16).dp),
        )
        Spacer(Modifier.height(AstraSpacing.M))
        Text("Task Score / 100", style = AstraTypography.Caption, color = AstraColors.TextSecondary)
        Spacer(Modifier.height(AstraSpacing.S))
        HorizontalBarChart(
            entries = results.map { r ->
                BarEntry(
                    label = r.model.displayName,
                    value = r.taskEvaluation.overallScore.toFloat(),
                    color = AstraColors.Success,
                    maxValue = 100f,
                )
            },
            modifier = Modifier.fillMaxWidth().height((results.size * 48 + 16).dp),
        )
    }
}

private data class BarEntry(
    val label: String,
    val value: Float,
    val color: Color,
    val maxValue: Float? = null,
)

@Composable
private fun HorizontalBarChart(
    entries: List<BarEntry>,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = AstraColors.TextSecondary
    val trackColor = AstraColors.Border
    val labelStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        color = labelColor,
    )

    Canvas(modifier = modifier.padding(end = 8.dp)) {
        if (entries.isEmpty()) return@Canvas

        val barHeight = 22.dp.toPx()
        val barGap = 26.dp.toPx()
        val labelWidth = 90.dp.toPx()
        val valueWidth = 36.dp.toPx()
        val chartWidth = size.width - labelWidth - valueWidth - 8.dp.toPx()
        val maxValue = entries.maxOfOrNull { it.maxValue ?: it.value }?.coerceAtLeast(1f) ?: 1f

        entries.forEachIndexed { i, entry ->
            val y = i * (barHeight + barGap) + 4.dp.toPx()
            val barY = y + (barGap / 2f)

            val labelMeasured = textMeasurer.measure(
                text = entry.label.take(14),
                style = labelStyle,
            )
            drawText(
                textLayoutResult = labelMeasured,
                topLeft = Offset(0f, barY + barHeight / 2 - labelMeasured.size.height / 2),
            )

            val trackX = labelWidth
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(trackX, barY),
                size = Size(chartWidth, barHeight),
                cornerRadius = CornerRadius(6.dp.toPx()),
            )

            val fillRatio = (entry.value / maxValue).coerceIn(0f, 1f)
            if (fillRatio > 0f) {
                drawRoundRect(
                    color = entry.color,
                    topLeft = Offset(trackX, barY),
                    size = Size(chartWidth * fillRatio, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
            }

            val valueText = if (entry.value > 0f) entry.value.toInt().toString() else "N/A"
            val valueMeasured = textMeasurer.measure(
                text = valueText,
                style = labelStyle.copy(fontWeight = FontWeight.SemiBold),
            )
            drawText(
                textLayoutResult = valueMeasured,
                topLeft = Offset(
                    trackX + chartWidth + 8.dp.toPx(),
                    barY + barHeight / 2 - valueMeasured.size.height / 2,
                ),
            )
        }
    }
}
