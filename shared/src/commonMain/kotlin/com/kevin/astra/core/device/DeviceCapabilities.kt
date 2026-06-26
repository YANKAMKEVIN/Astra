package com.kevin.astra.core.device

import com.kevin.astra.core.ai.InferenceBackend

interface DeviceCapabilityProvider {
    suspend fun getCapabilities(): DeviceCapabilities
}

data class DeviceCapabilities(
    val platform: String,
    val osVersion: String,
    val deviceModel: String,
    val cpuName: String,
    val gpuName: String,
    val npuAvailable: Boolean,
    val npuName: String,
    val totalMemoryMb: Long,
    val availableMemoryMb: Long,
    val storageAvailableGb: Double,
    val supportedBackends: List<InferenceBackend>,
    val supportedFeatures: List<SupportedFeature>,
)

enum class SupportedFeature(val label: String) {
    LocalAI("Local AI"),
    DocumentQA("Document QA"),
    Benchmark("Benchmark"),
    NPU("NPU"),
    GPU("GPU"),
    OfflineMode("Offline Mode"),
}

expect fun createDeviceCapabilityProvider(): DeviceCapabilityProvider
