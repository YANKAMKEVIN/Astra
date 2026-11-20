package com.kevin.astra.core.design

import androidx.compose.runtime.Composable

/**
 * Intercepts the system Back gesture/button while [enabled].
 * Android routes it through the activity's back dispatcher; iOS has no system Back button,
 * so the on-screen Back controls are the way back there.
 */
@Composable
expect fun AstraBackHandler(enabled: Boolean = true, onBack: () -> Unit)
