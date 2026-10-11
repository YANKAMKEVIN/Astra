package com.kevin.astra.presentation.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** iOS shows download progress in the app; background transfers need no notification permission. */
@Composable
actual fun rememberDownloadNotificationPermission(): DownloadNotificationPermission = remember {
    object : DownloadNotificationPermission {
        override val shouldAsk: Boolean = false
        override fun request(onResult: () -> Unit) = onResult()
        override fun markDeclined() = Unit
    }
}
