package com.kevin.astra.core.device

import com.kevin.astra.core.ai.InferenceBackend
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSNumber
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURL
import platform.Foundation.NSURLVolumeAvailableCapacityForImportantUsageKey
import platform.UIKit.UIDevice
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.value
import platform.darwin.HOST_VM_INFO64
import platform.darwin.KERN_SUCCESS
import platform.darwin.host_statistics64
import platform.darwin.integer_tVar
import platform.darwin.mach_host_self
import platform.darwin.mach_msg_type_number_tVar
import platform.darwin.vm_page_size
import platform.darwin.vm_statistics64_data_t

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
            availableMemoryMb = availableMemoryBytes().toMb(),
            storageAvailableGb = freeStorageBytes().toDouble() / (1024.0 * 1024.0 * 1024.0),
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

/** Free + inactive pages from the mach VM statistics — what the system can hand out now. 0 = unknown. */
@OptIn(ExperimentalForeignApi::class)
private fun availableMemoryBytes(): Long = memScoped {
    val stats = alloc<vm_statistics64_data_t>()
    val count = alloc<mach_msg_type_number_tVar>()
    count.value = (sizeOf<vm_statistics64_data_t>() / sizeOf<integer_tVar>()).toUInt()
    val result = host_statistics64(mach_host_self(), HOST_VM_INFO64, stats.ptr.reinterpret(), count.ptr)
    if (result != KERN_SUCCESS) return@memScoped 0L
    (stats.free_count.toLong() + stats.inactive_count.toLong()) * vm_page_size.toLong()
}

/**
 * Free space as iOS reports it in Settings › Storage: "important usage" capacity counts
 * purgeable caches the system would evict for a user-initiated download. Falls back to the
 * raw file-system free size, then 0 (= unknown).
 */
@OptIn(ExperimentalForeignApi::class)
private fun freeStorageBytes(): Long {
    val home = NSHomeDirectory()
    val important = runCatching {
        NSURL.fileURLWithPath(home)
            .resourceValuesForKeys(listOf(NSURLVolumeAvailableCapacityForImportantUsageKey), null)
            ?.get(NSURLVolumeAvailableCapacityForImportantUsageKey) as? NSNumber
    }.getOrNull()?.longLongValue
    if (important != null && important > 0) return important
    val raw = runCatching {
        NSFileManager.defaultManager.attributesOfFileSystemForPath(home, null)
            ?.get(NSFileSystemFreeSize) as? NSNumber
    }.getOrNull()?.longLongValue
    return (raw ?: 0L).coerceAtLeast(0L)
}

private const val UnknownValue = "Unknown"
private const val NotDetectedValue = "Not detected"

private fun Long.toMb(): Long =
    (this / (1024L * 1024L)).coerceAtLeast(0L)
