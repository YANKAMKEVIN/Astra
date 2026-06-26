package com.kevin.astra.core.ai

enum class AiModel(val label: String) {
    Mock("Mock Model"),
    Gemma("Gemma"),
    Phi("Phi"),
    Llama("Llama"),
    Qwen("Qwen"),
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
)

data class GenerationMetrics(
    val latencyMillis: Long,
    val timeToFirstTokenMillis: Long,
    val tokensGenerated: Int,
    val tokensPerSecond: Int,
    val memoryUsageMb: Int,
)
