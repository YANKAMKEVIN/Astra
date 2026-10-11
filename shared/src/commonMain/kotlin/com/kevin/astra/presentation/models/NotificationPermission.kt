package com.kevin.astra.presentation.models

import androidx.compose.runtime.Composable

/**
 * Asks for the right to post notifications at the moment it matters — the first model
 * download, whose progress lives in a notification — instead of a bare prompt at launch.
 */
interface DownloadNotificationPermission {
    /** True when the app should explain and ask before starting a download. */
    val shouldAsk: Boolean

    /** Shows the system prompt; [onResult] runs whatever the answer. */
    fun request(onResult: () -> Unit)

    /** Remembers the user declined our explanation, so we don't nag on every download. */
    fun markDeclined()
}

@Composable
expect fun rememberDownloadNotificationPermission(): DownloadNotificationPermission
