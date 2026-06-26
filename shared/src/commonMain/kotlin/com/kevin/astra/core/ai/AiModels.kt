package com.kevin.astra.core.ai

enum class AiModel(val label: String) {
    Mock("Mock Model"),
    Gemma("Gemma"),
    Phi("Phi"),
    Llama("Llama"),
    Qwen("Qwen"),
}

enum class ModelStatus(val label: String) {
    Installed("Installed"),
    Available("Available"),
    DownloadRequired("Download Required"),
    Unsupported("Unsupported"),
}

enum class ModelProvider(val label: String) {
    Google("Google"),
    Microsoft("Microsoft"),
    Meta("Meta"),
    Alibaba("Alibaba"),
    MistralAi("Mistral AI"),
    Mock("Mock"),
}

data class LocalModel(
    val id: String,
    val displayName: String,
    val provider: ModelProvider,
    val parameterCount: String,
    val quantization: String,
    val contextWindow: Int,
    val supportedBackends: List<InferenceBackend>,
    val minimumMemoryMb: Int,
    val status: ModelStatus,
    val runtimeModel: AiModel,
)

interface ModelCatalog {
    fun availableModels(): List<LocalModel>
    fun installedModels(): List<LocalModel>
    fun currentModel(): LocalModel
    fun selectModel(modelId: String): Boolean
    fun modelById(modelId: String): LocalModel?
}

enum class BackendStatus(val label: String) {
    Installed("Installed"),
    Available("Available"),
    ComingSoon("Coming Soon"),
    Unsupported("Unsupported"),
}

enum class BackendProvider(val label: String) {
    Astra("ASTRA"),
    Google("Google"),
    Microsoft("Microsoft"),
    Apple("Apple"),
    Ggml("GGML"),
    Qualcomm("Qualcomm"),
    Mock("Mock"),
}

enum class AccelerationTarget(val label: String) {
    Cpu("CPU"),
    Gpu("GPU"),
    Npu("NPU"),
    Ane("ANE"),
    Metal("Metal"),
    Nnapi("NNAPI"),
}

data class InferenceBackendInfo(
    val id: String,
    val displayName: String,
    val provider: BackendProvider,
    val supportedPlatforms: List<String>,
    val supportedModelFormats: List<String>,
    val accelerationTargets: List<AccelerationTarget>,
    val status: BackendStatus,
    val description: String,
    val runtimeBackend: InferenceBackend,
)

interface BackendCatalog {
    fun availableBackends(): List<InferenceBackendInfo>
    fun installedBackends(): List<InferenceBackendInfo>
    fun currentBackend(): InferenceBackendInfo
    fun selectBackend(backendId: String): Boolean
    fun backendById(backendId: String): InferenceBackendInfo?
}

enum class InferenceBackend(val label: String) {
    Mock("Mock Engine"),
    LiteRt("LiteRT"),
    OnnxRuntime("ONNX Runtime"),
    CoreMl("Core ML"),
    LlamaCpp("llama.cpp"),
}

enum class PromptIndustry(val label: String) {
    IndustrialMaintenance("Industrial Maintenance"),
    Aerospace("Aerospace"),
    Defense("Defense"),
    Energy("Energy"),
    Healthcare("Healthcare"),
}

data class PromptRequest(
    val prompt: String,
    val industry: PromptIndustry,
    val model: AiModel = AiModel.Mock,
    val backend: InferenceBackend = InferenceBackend.Mock,
    val maxTokens: Int = 512,
    val temperature: Double = 0.2,
)

data class GenerationResult(
    val text: String,
    val metrics: GenerationMetrics,
    val model: AiModel,
    val backend: InferenceBackend,
    val generatedAt: String,
    val runtimeInfo: GenerationRuntimeInfo = GenerationRuntimeInfo(),
)

data class GenerationMetrics(
    val latencyMillis: Long,
    val timeToFirstTokenMillis: Long,
    val tokensGenerated: Int,
    val tokensPerSecond: Int,
    val memoryUsageMb: Int,
)

enum class RuntimeMode(val label: String) {
    Real("Real Local Inference"),
    Fallback("Fallback Mock Inference"),
    Simulated("Simulated Local Inference"),
}

data class GenerationRuntimeInfo(
    val mode: RuntimeMode = RuntimeMode.Simulated,
    val modelLoadTimeMillis: Long = 0L,
    val inferenceLatencyMillis: Long = 0L,
    val totalExecutionTimeMillis: Long = 0L,
    val fallbackReason: String? = null,
)
