package com.kevin.astra.data.ai

import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.ai.ModelProvider
import com.kevin.astra.core.ai.ModelStatus

class DefaultModelCatalog(preInstalledIds: Set<String> = emptySet()) : ModelCatalog {
    private val baseModels = listOf(
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
            quantization = "4-bit (q4_block128)",
            contextWindow = 8_192,
            supportedBackends = listOf(InferenceBackend.LiteRtLm),
            minimumMemoryMb = 1_024,
            status = ModelStatus.DownloadRequired,
            runtimeModel = AiModel.Gemma,
            downloadUrl = "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q8_ekv1280.task",
        ),
        LocalModel(
            id = "gemma-3-4b",
            displayName = "Gemma 3 4B",
            provider = ModelProvider.Google,
            parameterCount = "4B",
            quantization = "int4",
            contextWindow = 8_192,
            supportedBackends = listOf(InferenceBackend.LiteRtLm),
            minimumMemoryMb = 3_072,
            status = ModelStatus.DownloadRequired,
            runtimeModel = AiModel.Gemma3_4B,
            // litert-community/Gemma3-4B-IT only has -web.task files (WASM, not Android-compatible)
            downloadUrl = null,
        ),
        LocalModel(
            id = "phi-4-mini",
            displayName = "Phi-4 Mini",
            provider = ModelProvider.Microsoft,
            parameterCount = "3.8B",
            quantization = "q8",
            contextWindow = 16_384,
            supportedBackends = listOf(InferenceBackend.LiteRtLm),
            minimumMemoryMb = 4_096,
            status = ModelStatus.DownloadRequired,
            runtimeModel = AiModel.Phi4Mini,
            // Verified filename: Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm (~3.9 GB)
            downloadUrl = "https://huggingface.co/litert-community/Phi-4-mini-instruct/resolve/main/Phi-4-mini-instruct_multi-prefill-seq_q8_ekv4096.litertlm",
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
            id = "qwen3-1-7b",
            displayName = "Qwen3 1.7B",
            provider = ModelProvider.Alibaba,
            parameterCount = "1.7B",
            quantization = "4-bit",
            contextWindow = 8_192,
            supportedBackends = listOf(InferenceBackend.LiteRtLm),
            minimumMemoryMb = 1_536,
            status = ModelStatus.DownloadRequired,
            runtimeModel = AiModel.Qwen3,
            // Verified filename: Qwen3_1.7B.litertlm (~1.9 GB)
            downloadUrl = "https://huggingface.co/litert-community/Qwen3-1.7B/resolve/main/Qwen3_1.7B.litertlm",
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
        LocalModel(
            id = "smollm2-360m",
            displayName = "SmolLM2 360M",
            provider = ModelProvider.HuggingFace,
            parameterCount = "360M",
            quantization = "4-bit",
            contextWindow = 4_096,
            supportedBackends = listOf(InferenceBackend.LiteRtLm),
            minimumMemoryMb = 512,
            status = ModelStatus.DownloadRequired,
            runtimeModel = AiModel.SmolLM,
            // Verified filename: SmolLM2_360M_instruct.litertlm (~356 MB)
            downloadUrl = "https://huggingface.co/litert-community/SmolLM2-360M-Instruct/resolve/main/SmolLM2_360M_instruct.litertlm",
        ),
        LocalModel(
            id = "llama-3-2-1b",
            displayName = "Llama 3.2 1B",
            provider = ModelProvider.Meta,
            parameterCount = "1B",
            quantization = "4-bit",
            contextWindow = 8_192,
            supportedBackends = listOf(InferenceBackend.LiteRtLm, InferenceBackend.LlamaCpp),
            minimumMemoryMb = 1_024,
            // litert-community/Llama-3.2-1B-Instruct not publicly available
            status = ModelStatus.DownloadRequired,
            runtimeModel = AiModel.Llama3_2,
            downloadUrl = null,
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
    )

    private val statusOverrides = mutableMapOf<String, ModelStatus>().apply {
        preInstalledIds
            .filter { id -> baseModels.any { it.id == id && it.status != ModelStatus.Installed } }
            .forEach { id -> put(id, ModelStatus.Installed) }
    }
    private var currentModelId: String = baseModels.first { it.status == ModelStatus.Installed }.id

    override fun availableModels(): List<LocalModel> =
        baseModels.map { model ->
            val override = statusOverrides[model.id]
            if (override != null) model.copy(status = override) else model
        }

    override fun installedModels(): List<LocalModel> =
        availableModels().filter { it.status == ModelStatus.Installed }

    override fun currentModel(): LocalModel =
        modelById(currentModelId) ?: installedModels().first()

    override fun selectModel(modelId: String): Boolean {
        val model = modelById(modelId) ?: return false
        if (model.status == ModelStatus.Unsupported) return false
        currentModelId = model.id
        return true
    }

    override fun modelById(modelId: String): LocalModel? =
        availableModels().firstOrNull { it.id == modelId }

    override fun updateModelStatus(modelId: String, status: ModelStatus): Boolean {
        if (baseModels.none { it.id == modelId }) return false
        statusOverrides[modelId] = status
        return true
    }
}
