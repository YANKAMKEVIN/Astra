package com.kevin.astra.presentation.models

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private const val PREFS = "astra_permissions"
private const val KEY_ASKED = "download_notifications_asked"

@Composable
actual fun rememberDownloadNotificationPermission(): DownloadNotificationPermission {
    val context = LocalContext.current
    // Bumped after each answer so shouldAsk is re-read.
    var revision by remember { mutableIntStateOf(0) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        context.markAsked()
        revision++
        pending?.invoke()
        pending = null
    }
    return remember(revision) {
        object : DownloadNotificationPermission {
            override val shouldAsk: Boolean =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED &&
                    !context.wasAsked()

            override fun request(onResult: () -> Unit) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return onResult()
                pending = onResult
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            override fun markDeclined() {
                context.markAsked()
                revision++
            }
        }
    }
}

private fun Context.wasAsked(): Boolean =
    getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ASKED, false)

private fun Context.markAsked() {
    getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ASKED, true).apply()
}
