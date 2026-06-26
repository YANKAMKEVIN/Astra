package com.kevin.astra.data.ai

import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.ai.ModelProvider
import com.kevin.astra.core.ai.ModelStatus

class DefaultModelCatalog : ModelCatalog {
    private val models = listOf(
        LocalModel(
            id = "mock-model",
            displayName = "Mock Model",
            provider = ModelProvider.Mock,
            parameterCount = "Simulated",
            quantization = "4-bit",
            contextWindow = 4_096,
            supportedBackends = listOf(InferenceBackend.Mock),
            minimumMemoryMb = 128,
            status = ModelStatus.Installed,
            runtimeModel = AiModel.Mock,
        ),
        LocalModel(
            id = "gemma-3-1b",
            displayName = "Gemma 3 1B",
            provider = ModelProvider.Google,
            parameterCount = "1B",
            quantization = "4-bit",
            contextWindow = 8_192,
            supportedBackends = listOf(InferenceBackend.LiteRt, InferenceBackend.OnnxRuntime),
            minimumMemoryMb = 1_024,
            status = ModelStatus.Available,
            runtimeModel = AiModel.Gemma,
        ),
        LocalModel(
            id = "phi-3-mini",
            displayName = "Phi-3 Mini",
            provider = ModelProvider.Microsoft,
            parameterCount = "3.8B",
            quantization = "4-bit",
            contextWindow = 4_096,
            supportedBackends = listOf(InferenceBackend.OnnxRuntime),
            minimumMemoryMb = 2_048,
            status = ModelStatus.Available,
            runtimeModel = AiModel.Phi,
        ),
        LocalModel(
            id = "llama-3-2-3b",
            displayName = "Llama 3.2 3B",
            provider = ModelProvider.Meta,
            parameterCount = "3B",
            quantization = "4-bit",
            contextWindow = 8_192,
            supportedBackends = listOf(InferenceBackend.LlamaCpp, InferenceBackend.OnnxRuntime),
            minimumMemoryMb = 2_560,
            status = ModelStatus.Available,
            runtimeModel = AiModel.Llama,
        ),
        LocalModel(
            id = "qwen-2-5-1-5b",
            displayName = "Qwen 2.5 1.5B",
            provider = ModelProvider.Alibaba,
            parameterCount = "1.5B",
            quantization = "4-bit",
            contextWindow = 8_192,
            supportedBackends = listOf(InferenceBackend.OnnxRuntime),
            minimumMemoryMb = 1_536,
            status = ModelStatus.Available,
            runtimeModel = AiModel.Qwen,
        ),
    )

    private var currentModelId: String = models.first { it.status == ModelStatus.Installed }.id

    override fun availableModels(): List<LocalModel> = models

    override fun installedModels(): List<LocalModel> =
        models.filter { it.status == ModelStatus.Installed }

    override fun currentModel(): LocalModel =
        modelById(currentModelId) ?: installedModels().first()

    override fun selectModel(modelId: String): Boolean {
        val model = modelById(modelId) ?: return false
        if (model.status != ModelStatus.Installed) return false
        currentModelId = model.id
        return true
    }

    override fun modelById(modelId: String): LocalModel? =
        models.firstOrNull { it.id == modelId }
}
