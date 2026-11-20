package com.kevin.astra.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kevin.astra.core.navigation.AstraDestination

/** Line icon used for a secondary destination (Tools sheet, Home quick actions). */
fun destinationIcon(destination: AstraDestination): ImageVector = when (destination) {
    AstraDestination.VoiceAssistant -> AstraIcons.Mic
    AstraDestination.VisionAssistant -> AstraIcons.Camera
    AstraDestination.History -> AstraIcons.Clock
    AstraDestination.Demo -> AstraIcons.Play
    AstraDestination.Models -> AstraIcons.Cube
    else -> AstraIcons.Sparkle
}

/** One-line description of a secondary destination. */
fun destinationSubtitle(destination: AstraDestination): String = when (destination) {
    AstraDestination.VoiceAssistant -> "Speech-to-text & TTS"
    AstraDestination.VisionAssistant -> "Analyze images on device"
    AstraDestination.History -> "Past conversations"
    AstraDestination.Demo -> "Guided offline demo"
    AstraDestination.Models -> "Manage models & runtimes"
    else -> "On-device"
}

/** Glass tile opening a destination: icon chip, chevron, title and subtitle. */
@Composable
fun AstraDestinationTile(
    destination: AstraDestination,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = destination.label,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(AstraColors.Surface.copy(alpha = 0.6f))
            .border(1.dp, AstraColors.GlassEdge.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(AstraSpacing.M),
        verticalArrangement = Arrangement.spacedBy(AstraSpacing.S),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AstraColors.Secondary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                AstraIcon(icon = destinationIcon(destination), tint = AstraColors.Secondary, size = 20.dp)
            }
            AstraIcon(icon = AstraIcons.ChevronRight, tint = AstraColors.TextDisabled, size = 16.dp)
        }
        Text(
            text = title,
            style = AstraTypography.Body,
            color = AstraColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = destinationSubtitle(destination),
            style = AstraTypography.Caption,
            color = AstraColors.TextDisabled,
        )
    }
}
