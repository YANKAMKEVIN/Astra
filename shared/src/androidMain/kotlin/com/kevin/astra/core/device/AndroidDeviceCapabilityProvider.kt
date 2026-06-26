package com.kevin.astra.core.device

import android.os.Build
import android.os.Environment
import com.kevin.astra.core.ai.InferenceBackend

actual fun createDeviceCapabilityProvider(): DeviceCapabilityProvider =
    AndroidDeviceCapabilityProvider()

class AndroidDeviceCapabilityProvider : DeviceCapabilityProvider {
    override suspend fun getCapabilities(): DeviceCapabilities {
        val runtime = Runtime.getRuntime()
        val maxMemoryMb = runtime.maxMemory().toMb()
        val availableMemoryMb = (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()).toMb()
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
            totalMemoryMb = maxMemoryMb,
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
