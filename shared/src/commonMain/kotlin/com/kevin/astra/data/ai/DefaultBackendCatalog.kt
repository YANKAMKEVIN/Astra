package com.kevin.astra.data.ai

import com.kevin.astra.core.ai.AccelerationTarget
import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.BackendProvider
import com.kevin.astra.core.ai.BackendStatus
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.InferenceBackendInfo

class DefaultBackendCatalog(
    private val statusOverrides: Map<String, BackendStatus> = emptyMap(),
) : BackendCatalog {
    private val backends = listOf(
        InferenceBackendInfo(
            id = "mock-engine",
            displayName = "Mock Engine",
            provider = BackendProvider.Mock,
            supportedPlatforms = listOf("Android", "iOS"),
            supportedModelFormats = listOf("Simulated"),
            accelerationTargets = listOf(AccelerationTarget.Cpu),
            status = statusFor("mock-engine", BackendStatus.Installed),
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
            status = statusFor("litert", BackendStatus.Available),
            description = "Google on-device inference runtime for Android local AI execution with Mock fallback when no local model is available.",
            runtimeBackend = InferenceBackend.LiteRt,
        ),
        InferenceBackendInfo(
            id = "litert-lm",
            displayName = "LiteRT-LM",
            provider = BackendProvider.Google,
            supportedPlatforms = listOf("Android"),
            supportedModelFormats = listOf("LiteRT-LM bundle", "TFLite", "SentencePiece tokenizer"),
            accelerationTargets = listOf(AccelerationTarget.Cpu, AccelerationTarget.Gpu, AccelerationTarget.Nnapi, AccelerationTarget.Npu),
            status = statusFor("litert-lm", BackendStatus.ModelRequired),
            description = "Generative SLM runtime foundation for tokenizer-driven local text generation on Android.",
            runtimeBackend = InferenceBackend.LiteRtLm,
        ),
        InferenceBackendInfo(
            id = "onnx-runtime",
            displayName = "ONNX Runtime",
            provider = BackendProvider.Microsoft,
            supportedPlatforms = listOf("Android"),
            supportedModelFormats = listOf("ONNX"),
            accelerationTargets = listOf(AccelerationTarget.Cpu, AccelerationTarget.Gpu, AccelerationTarget.Nnapi),
            status = statusFor("onnx-runtime", BackendStatus.Available),
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
            status = statusFor("core-ml", BackendStatus.Available),
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
            status = statusFor("llama-cpp", BackendStatus.ComingSoon),
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

    private fun statusFor(id: String, defaultStatus: BackendStatus): BackendStatus =
        statusOverrides[id] ?: defaultStatus
}
