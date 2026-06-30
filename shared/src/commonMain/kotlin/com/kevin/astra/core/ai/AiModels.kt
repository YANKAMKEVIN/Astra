package com.kevin.astra.core.ai

enum class AiModel(val label: String, val filesystemId: String) {
    Mock("Mock Model", "mock-model"),
    Gemma("Gemma", "gemma-3-1b"),
    Gemma3_4B("Gemma 3 4B", "gemma-3-4b"),
    Phi("Phi", "phi-3-mini"),
    Phi4Mini("Phi-4 Mini", "phi-4-mini"),
    Llama("Llama", "llama-3-2-3b"),
    Llama3_2("Llama 3.2 1B", "llama-3-2-1b"),
    Qwen("Qwen", "qwen-2-5-1-5b"),
    Qwen3("Qwen3", "qwen3-1-7b"),
    SmolLM("SmolLM2", "smollm2-360m"),
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
    HuggingFace("HuggingFace"),
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
    val downloadUrl: String? = null,
)

interface ModelCatalog {
    fun availableModels(): List<LocalModel>
    fun installedModels(): List<LocalModel>
    fun currentModel(): LocalModel
    fun selectModel(modelId: String): Boolean
    fun modelById(modelId: String): LocalModel?
    fun updateModelStatus(modelId: String, status: ModelStatus): Boolean
}

enum class BackendStatus(val label: String) {
    Installed("Installed"),
    Available("Available"),
    ModelRequired("Model Required"),
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
    LiteRtLm("LiteRT-LM"),
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
    LiteRtTensor("LiteRT Tensor Runtime"),
    LiteRtLmGenerative("LiteRT-LM Generative Runtime"),
    ModelMissing("Model Missing"),
    UnsupportedPlatform("Unsupported Platform"),
    Fallback("Mock Fallback"),
    Simulated("Simulated Local Inference"),
}

data class GenerationRuntimeInfo(
    val mode: RuntimeMode = RuntimeMode.Simulated,
    val modelLoadTimeMillis: Long = 0L,
    val inferenceLatencyMillis: Long = 0L,
    val totalExecutionTimeMillis: Long = 0L,
    val fallbackReason: String? = null,
)
