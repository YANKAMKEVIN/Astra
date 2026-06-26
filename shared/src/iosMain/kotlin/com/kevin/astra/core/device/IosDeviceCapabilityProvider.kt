package com.kevin.astra.core.device

import com.kevin.astra.core.ai.InferenceBackend
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice

actual fun createDeviceCapabilityProvider(): DeviceCapabilityProvider =
    IosDeviceCapabilityProvider()

class IosDeviceCapabilityProvider : DeviceCapabilityProvider {
    override suspend fun getCapabilities(): DeviceCapabilities {
        val device = UIDevice.currentDevice
        val processInfo = NSProcessInfo.processInfo
        val totalMemoryMb = processInfo.physicalMemory.toLong().toMb()

        return DeviceCapabilities(
            platform = "iOS",
            osVersion = "${device.systemName} ${device.systemVersion}",
            deviceModel = device.model.ifBlank { UnknownValue },
            cpuName = processInfo.processorCount.toString() + " logical cores",
            gpuName = NotDetectedValue,
            npuAvailable = false,
            npuName = NotDetectedValue,
            totalMemoryMb = totalMemoryMb,
            availableMemoryMb = 0L,
            storageAvailableGb = 0.0,
            supportedBackends = listOf(
                InferenceBackend.Mock,
                InferenceBackend.CoreMl,
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
