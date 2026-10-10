package com.kevin.astra.core.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import com.kevin.astra.app.di.androidAppContext
import com.kevin.astra.core.ai.InferenceBackend

actual fun createDeviceCapabilityProvider(): DeviceCapabilityProvider =
    AndroidDeviceCapabilityProvider()

class AndroidDeviceCapabilityProvider : DeviceCapabilityProvider {
    override suspend fun getCapabilities(): DeviceCapabilities {
        // Device RAM, not the JVM heap cap (Runtime.maxMemory() is ~256–512 MB and would
        // flag every model as too large). Falls back to 0 = unknown if Koin isn't up yet.
        val memoryInfo = runCatching {
            val activityManager = androidAppContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        }.getOrNull()
        val totalMemoryMb = memoryInfo?.totalMem?.toMb() ?: 0L
        val availableMemoryMb = memoryInfo?.availMem?.toMb() ?: 0L
        val storageAvailableGb = Environment.getDataDirectory().usableSpace.toGb()

        return DeviceCapabilities(
            platform = "Android",
            osVersion = "Android ${Build.VERSION.RELEASE ?: UnknownValue} (API ${Build.VERSION.SDK_INT})",
            deviceModel = listOfNotNull(Build.MANUFACTURER, Build.MODEL)
                .joinToString(separator = " ")
                .ifBlank { UnknownValue },
            cpuName = Build.SUPPORTED_ABIS.firstOrNull() ?: UnknownValue,
            gpuName = NotDetectedValue,
            npuAvailable = false,
            npuName = NotDetectedValue,
            totalMemoryMb = totalMemoryMb,
            availableMemoryMb = availableMemoryMb,
            storageAvailableGb = storageAvailableGb,
            supportedBackends = listOf(
                InferenceBackend.Mock,
                InferenceBackend.LiteRt,
                InferenceBackend.OnnxRuntime,
            ),
            supportedFeatures = listOf(
                SupportedFeature.LocalAI,
                SupportedFeature.DocumentQA,
                SupportedFeature.Benchmark,
                SupportedFeature.OfflineMode,
            ),
        )
    }
}

private const val UnknownValue = "Unknown"
private const val NotDetectedValue = "Not detected"

private fun Long.toMb(): Long =
    (this / (1024L * 1024L)).coerceAtLeast(0L)

private fun Long.toGb(): Double =
    this / (1024.0 * 1024.0 * 1024.0)
