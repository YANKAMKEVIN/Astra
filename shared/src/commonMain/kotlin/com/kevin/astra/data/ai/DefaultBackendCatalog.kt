package com.kevin.astra.data.ai

import com.kevin.astra.core.ai.AccelerationTarget
import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.BackendProvider
import com.kevin.astra.core.ai.BackendStatus
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.InferenceBackendInfo

class DefaultBackendCatalog : BackendCatalog {
    private val backends = listOf(
        InferenceBackendInfo(
            id = "mock-engine",
            displayName = "Mock Engine",
            provider = BackendProvider.Mock,
            supportedPlatforms = listOf("Android", "iOS"),
            supportedModelFormats = listOf("Simulated"),
            accelerationTargets = listOf(AccelerationTarget.Cpu),
            status = BackendStatus.Installed,
            description = "Deterministic local mock backend used for offline development and demos.",
            runtimeBackend = InferenceBackend.Mock,
        ),
        InferenceBackendInfo(
            id = "litert",
            displayName = "LiteRT",
            provider = BackendProvider.Google,
            supportedPlatforms = listOf("Android"),
            supportedModelFormats = listOf("TFLite", "LiteRT"),
            accelerationTargets = listOf(AccelerationTarget.Cpu, AccelerationTarget.Gpu, AccelerationTarget.Nnapi, AccelerationTarget.Npu),
            status = BackendStatus.Available,
            description = "Google on-device inference runtime planned for Android local AI execution.",
            runtimeBackend = InferenceBackend.LiteRt,
        ),
        InferenceBackendInfo(
            id = "onnx-runtime",
            displayName = "ONNX Runtime",
            provider = BackendProvider.Microsoft,
            supportedPlatforms = listOf("Android"),
            supportedModelFormats = listOf("ONNX"),
            accelerationTargets = listOf(AccelerationTarget.Cpu, AccelerationTarget.Gpu, AccelerationTarget.Nnapi),
            status = BackendStatus.Available,
            description = "ONNX execution backend planned for portable local model support.",
            runtimeBackend = InferenceBackend.OnnxRuntime,
        ),
        InferenceBackendInfo(
            id = "core-ml",
            displayName = "Core ML",
            provider = BackendProvider.Apple,
            supportedPlatforms = listOf("iOS"),
            supportedModelFormats = listOf("MLPackage", "MLModel"),
            accelerationTargets = listOf(AccelerationTarget.Cpu, AccelerationTarget.Gpu, AccelerationTarget.Ane, AccelerationTarget.Metal),
            status = BackendStatus.Available,
            description = "Apple inference backend planned for iOS local model execution.",
            runtimeBackend = InferenceBackend.CoreMl,
        ),
        InferenceBackendInfo(
            id = "llama-cpp",
            displayName = "llama.cpp",
            provider = BackendProvider.Ggml,
            supportedPlatforms = listOf("Android", "iOS"),
            supportedModelFormats = listOf("GGUF"),
            accelerationTargets = listOf(AccelerationTarget.Cpu, AccelerationTarget.Gpu, AccelerationTarget.Metal),
            status = BackendStatus.ComingSoon,
            description = "GGUF-focused backend planned for future quantized model experiments.",
            runtimeBackend = InferenceBackend.LlamaCpp,
        ),
    )

    private var currentBackendId: String = backends.first { it.status == BackendStatus.Installed }.id

    override fun availableBackends(): List<InferenceBackendInfo> = backends

    override fun installedBackends(): List<InferenceBackendInfo> =
        backends.filter { it.status == BackendStatus.Installed }

    override fun currentBackend(): InferenceBackendInfo =
        backendById(currentBackendId) ?: installedBackends().first()

    override fun selectBackend(backendId: String): Boolean {
        val backend = backendById(backendId) ?: return false
        if (backend.status != BackendStatus.Installed) return false
        currentBackendId = backend.id
        return true
    }

    override fun backendById(backendId: String): InferenceBackendInfo? =
        backends.firstOrNull { it.id == backendId }
}
