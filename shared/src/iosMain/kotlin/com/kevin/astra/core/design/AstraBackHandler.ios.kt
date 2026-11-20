package com.kevin.astra.core.design

import androidx.compose.runtime.Composable

@Composable
actual fun AstraBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No system Back on iOS: navigation goes through the on-screen Back controls.
}
