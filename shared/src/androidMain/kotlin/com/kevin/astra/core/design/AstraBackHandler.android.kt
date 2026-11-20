package com.kevin.astra.core.design

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun AstraBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
