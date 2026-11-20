package com.kevin.astra.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Snackbar message with an optional leading line icon (e.g. a tick for a success). */
data class AstraSnackbarVisuals(
    override val message: String,
    val icon: ImageVector? = null,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration =
        if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite,
) : SnackbarVisuals

/** ASTRA-styled snackbar; draws the icon of [AstraSnackbarVisuals] before the message. */
@Composable
fun AstraSnackbar(data: SnackbarData) {
    val icon = (data.visuals as? AstraSnackbarVisuals)?.icon
    val actionLabel = data.visuals.actionLabel
    Snackbar(
        containerColor = AstraColors.SurfaceElevated,
        contentColor = AstraColors.TextPrimary,
        action = actionLabel?.let {
            {
                TextButton(onClick = { data.performAction() }) {
                    Text(it, color = AstraColors.Primary)
                }
            }
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S),
        ) {
            if (icon != null) AstraIcon(icon = icon, tint = AstraColors.Success, size = 18.dp)
            Text(data.visuals.message)
        }
    }
}
