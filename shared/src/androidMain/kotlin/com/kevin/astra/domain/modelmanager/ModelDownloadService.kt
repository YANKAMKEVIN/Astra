package com.kevin.astra.domain.modelmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Keeps a model download alive while the screen is off or the app is in the background.
 *
 * The download itself still runs in [AndroidModelDownloadManager]; this service only makes the
 * process a foreground "data sync" job (so Android doesn't cut its network on lock — that
 * aborted downloads with "Software caused connection abort"), holds a partial wake lock, and
 * shows a progress notification with a Cancel action. It stops itself once the manager leaves
 * the Downloading state.
 */
class ModelDownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastNotifiedPercent = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = GlobalContext.getOrNull()?.getOrNull<ModelDownloadManager>()
        val modelId = intent?.getStringExtra(EXTRA_MODEL_ID)
        if (intent?.action == ACTION_CANCEL && modelId != null) {
            manager?.cancel(modelId)
            stopSelfCleanly()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_DISPLAY_NAME) ?: "model"
        ensureChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(title, modelId, percent = 0, indeterminate = true),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
        )
        acquireWakeLock()

        if (manager == null) {
            stopSelfCleanly()
            return START_NOT_STICKY
        }
        scope.launch {
            manager.downloadState.collect { state ->
                if (state is ModelDownloadState.Downloading) {
                    // Throttle: one notification update per percent, not per 64 KB chunk.
                    if (state.progressPercent != lastNotifiedPercent) {
                        lastNotifiedPercent = state.progressPercent
                        notificationManager().notify(
                            NOTIFICATION_ID,
                            buildNotification(title, state.modelId, state.progressPercent, indeterminate = state.totalMb <= 0f),
                        )
                    }
                } else {
                    stopSelfCleanly()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        super.onDestroy()
    }

    private fun stopSelfCleanly() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "astra:model-download")
            .apply { acquire(WAKE_LOCK_TIMEOUT_MS) }
    }

    private fun buildNotification(title: String, modelId: String?, percent: Int, indeterminate: Boolean) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading $title")
            .setContentText(if (indeterminate) "Starting…" else "$percent%")
            .setProgress(100, percent, indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openAppIntent())
            .apply {
                if (modelId != null) {
                    addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent(modelId))
                }
            }
            .build()

    private fun openAppIntent(): PendingIntent? =
        packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
            PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

    private fun cancelIntent(modelId: String): PendingIntent =
        PendingIntent.getService(
            this,
            1,
            Intent(this, ModelDownloadService::class.java)
                .setAction(ACTION_CANCEL)
                .putExtra(EXTRA_MODEL_ID, modelId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        notificationManager().createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Model downloads", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Progress of on-device AI model downloads" },
        )
    }

    private fun notificationManager() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "astra_model_downloads"
        private const val NOTIFICATION_ID = 4_210
        private const val ACTION_CANCEL = "com.kevin.astra.action.CANCEL_MODEL_DOWNLOAD"
        private const val EXTRA_MODEL_ID = "modelId"
        private const val EXTRA_DISPLAY_NAME = "displayName"
        /** Safety net in case the service is never stopped; largest models take minutes, not hours. */
        private const val WAKE_LOCK_TIMEOUT_MS = 2 * 60 * 60 * 1000L

        /** Best effort: if Android refuses a foreground start, the download still runs in-process. */
        fun start(context: Context, modelId: String, displayName: String): Boolean = runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ModelDownloadService::class.java)
                    .putExtra(EXTRA_MODEL_ID, modelId)
                    .putExtra(EXTRA_DISPLAY_NAME, displayName),
            )
        }.isSuccess
    }
}
