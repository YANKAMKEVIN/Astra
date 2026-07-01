package com.kevin.astra.core.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kevin.astra.core.navigation.AstraDestination

enum class AstraButtonStyle {
    Primary,
    Secondary,
    Danger,
    Ghost,
}

@Composable
fun AstraCard(
    title: String,
    subtitle: String? = null,
    status: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AstraColors.Surface),
        border = BorderStroke(1.dp, AstraColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(AstraSpacing.L)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = AstraTypography.Title,
                        color = AstraColors.TextPrimary,
                    )
                    if (subtitle != null) {
                        Spacer(Modifier.height(AstraSpacing.XS))
                        Text(
                            text = subtitle,
                            style = AstraTypography.Caption,
                            color = AstraColors.TextSecondary,
                        )
                    }
                }
                if (status != null) {
                    AstraChip(label = status, color = AstraColors.Secondary)
                }
            }
            content()
        }
    }
}

@Composable
fun AstraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AstraButtonStyle = AstraButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val containerColor = when (style) {
        AstraButtonStyle.Primary -> AstraColors.Primary
        AstraButtonStyle.Secondary -> AstraColors.SurfaceElevated
        AstraButtonStyle.Danger -> AstraColors.Error
        AstraButtonStyle.Ghost -> Color.Transparent
    }

    if (style == AstraButtonStyle.Ghost) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, AstraColors.Border),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AstraColors.TextPrimary,
                disabledContentColor = AstraColors.TextDisabled,
            ),
        ) {
            Text(text)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = AstraColors.TextPrimary,
                disabledContainerColor = AstraColors.SurfaceElevated,
                disabledContentColor = AstraColors.TextDisabled,
            ),
        ) {
            Text(text)
        }
    }
}

@Composable
fun AstraChip(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = AstraColors.Primary,
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.38f), RoundedCornerShape(12.dp))
            .padding(horizontal = AstraSpacing.S, vertical = AstraSpacing.XS),
    ) {
        Text(
            text = label,
            style = AstraTypography.Caption,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun AstraMetricCard(
    value: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(AstraColors.SurfaceElevated, RoundedCornerShape(16.dp))
            .border(1.dp, AstraColors.Border, RoundedCornerShape(16.dp))
            .padding(AstraSpacing.M),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = AstraTypography.Title,
                color = AstraColors.TextPrimary,
            )
            if (unit.isNotBlank()) {
                Text(
                    text = " $unit",
                    style = AstraTypography.Metric,
                    color = AstraColors.Secondary,
                )
            }
        }
        Spacer(Modifier.height(AstraSpacing.XS))
        Text(
            text = label,
            style = AstraTypography.Caption,
            color = AstraColors.TextSecondary,
        )
    }
}

@Composable
fun AstraErrorView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AstraColors.Error.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .border(1.dp, AstraColors.Error.copy(alpha = 0.24f), RoundedCornerShape(24.dp))
            .padding(AstraSpacing.L),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = AstraTypography.Title,
            color = AstraColors.Error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AstraSpacing.S))
        Text(
            text = message,
            style = AstraTypography.Body,
            color = AstraColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(AstraSpacing.M))
            AstraButton(
                text = "Retry",
                onClick = onRetry,
                style = AstraButtonStyle.Danger,
            )
        }
    }
}

@Composable
fun AstraEmptyView(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AstraColors.SurfaceElevated, RoundedCornerShape(24.dp))
            .border(1.dp, AstraColors.Border, RoundedCornerShape(24.dp))
            .padding(AstraSpacing.L),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(AstraColors.Secondary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(AstraColors.Secondary, RoundedCornerShape(50)),
            )
        }
        Spacer(Modifier.height(AstraSpacing.M))
        Text(
            text = title,
            style = AstraTypography.Title,
            color = AstraColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AstraSpacing.S))
        Text(
            text = message,
            style = AstraTypography.Body,
            color = AstraColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AstraDemoModeIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(AstraColors.Secondary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .border(1.dp, AstraColors.Secondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = AstraSpacing.S, vertical = AstraSpacing.XS),
    ) {
        Text(
            text = "Offline Demo Mode",
            style = AstraTypography.Caption,
            color = AstraColors.Secondary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun AstraNavigationBar(
    selectedDestination: AstraDestination,
    onDestinationSelected: (AstraDestination) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AstraColors.Surface)
            .border(1.dp, AstraColors.Border)
            .navigationBarsPadding()
            .padding(horizontal = AstraSpacing.S, vertical = AstraSpacing.S),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        AstraDestination.primaryNavDestinations.forEach { destination ->
            NavBarItem(
                label = destination.shortLabel,
                selected = destination == selectedDestination,
                onClick = { onDestinationSelected(destination) },
            )
        }
    }
}

@Composable
private fun NavBarItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .background(
                color = if (selected) AstraColors.Primary.copy(alpha = 0.16f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(
                    if (selected) AstraColors.Secondary else AstraColors.TextDisabled,
                    RoundedCornerShape(50),
                ),
        )
        Spacer(Modifier.height(AstraSpacing.XS))
        Text(
            text = label,
            style = AstraTypography.Caption,
            color = if (selected) AstraColors.TextPrimary else AstraColors.TextSecondary,
        )
    }
}

@Composable
fun DemoModeBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AstraColors.Warning.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .border(1.dp, AstraColors.Warning.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = AstraSpacing.M, vertical = AstraSpacing.S),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "⚡ OFFLINE DEMO MODE — responses are simulated",
            style = AstraTypography.Caption,
            color = AstraColors.Warning,
            textAlign = TextAlign.Center,
        )
    }
}
