package com.kevin.astra.data.ai

import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.ai.ModelProvider
import com.kevin.astra.core.ai.ModelStatus

class DefaultModelCatalog(
    preInstalledIds: Set<String> = emptySet(),
    usableBackends: Set<InferenceBackend> = InferenceBackend.entries.toSet(),
) : ModelCatalog {
    private val allModels = listOf(
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
            id = "gemma-4-e2b",
            displayName = "Gemma 4 E2B",
            provider = ModelProvider.Google,
            parameterCount = "E2B",
            quantization = "int4",
            contextWindow = 8_192,
            supportedBackends = listOf(InferenceBackend.LiteRtLm),
            minimumMemoryMb = 4_096,
            status = ModelStatus.DownloadRequired,
            runtimeModel = AiModel.Gemma4E2B,
            // Ungated Apache-2.0 LiteRT-LM build; vendor-neutral CPU .litertlm (~2.6 GB). This is the
            // model the iOS LiteRTLMSwift bridge's generate() targets (Gemma-4 turn markers).
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm",
        ),
        LocalModel(
            id = "gemma-4-e4b",
            displayName = "Gemma 4 E4B",
            provider = ModelProvider.Google,
            parameterCount = "E4B",
            quantization = "int4",
            contextWindow = 8_192,
            supportedBackends = listOf(InferenceBackend.LiteRtLm),
            minimumMemoryMb = 6_144,
            status = ModelStatus.DownloadRequired,
            runtimeModel = AiModel.Gemma4E4B,
            // Ungated Apache-2.0 LiteRT-LM build; vendor-neutral CPU .litertlm (~3.7 GB).
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/main/gemma-4-E4B-it.litertlm",
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
            id = "smollm2-135m",
            displayName = "SmolLM2 135M",
            provider = ModelProvider.HuggingFace,
            parameterCount = "135M",
            quantization = "int8",
            contextWindow = 4_096,
            supportedBackends = listOf(InferenceBackend.LiteRtLm),
            minimumMemoryMb = 512,
            status = ModelStatus.DownloadRequired,
            runtimeModel = AiModel.SmolLM135M,
            // Verified ungated CPU .litertlm (~143 MB).
            downloadUrl = "https://huggingface.co/litert-community/SmolLM2-135M-Instruct/resolve/main/SmolLM2_135M_Instruct.litertlm",
        ),
        LocalModel(
            id = "smollm3-3b",
            displayName = "SmolLM3 3B",
            provider = ModelProvider.HuggingFace,
            parameterCount = "3B",
            quantization = "int8",
            contextWindow = 8_192,
            supportedBackends = listOf(InferenceBackend.LiteRtLm),
            minimumMemoryMb = 3_072,
            status = ModelStatus.DownloadRequired,
            runtimeModel = AiModel.SmolLM3,
            // Verified ungated CPU .litertlm (~3.1 GB).
            downloadUrl = "https://huggingface.co/litert-community/SmolLM3-3B/resolve/main/SmolLM3-3B.litertlm",
        ),
        LocalModel(
            id = "qwen3-0-6b",
            displayName = "Qwen3 0.6B",
            provider = ModelProvider.Alibaba,
            parameterCount = "0.6B",
            quantization = "4-bit (q4_block32)",
            contextWindow = 4_096,
            supportedBackends = listOf(InferenceBackend.LiteRtLm),
            minimumMemoryMb = 768,
            status = ModelStatus.DownloadRequired,
            runtimeModel = AiModel.Qwen3_0_6B,
            // Verified ungated CPU .litertlm (~347 MB).
            downloadUrl = "https://huggingface.co/litert-community/Qwen3-0.6B-int4/resolve/main/qwen3_0.6b_nothink_q4_block32_ekv1280.litertlm",
        ),
        LocalModel(
            id = "qwen-2-5-coder-1-5b",
            displayName = "Qwen2.5 Coder 1.5B",
            provider = ModelProvider.Alibaba,
            parameterCount = "1.5B",
            quantization = "int4",
            contextWindow = 8_192,
            supportedBackends = listOf(InferenceBackend.LiteRtLm),
            minimumMemoryMb = 1_536,
            status = ModelStatus.DownloadRequired,
            runtimeModel = AiModel.Qwen25Coder,
            // Verified ungated CPU .litertlm (~1.1 GB).
            downloadUrl = "https://huggingface.co/litert-community/Qwen2.5-Coder-1.5B-Instruct/resolve/main/Qwen2.5-Coder-1.5B-Instruct_int4.litertlm",
        ),
        LocalModel(
            id = "phi-4-mini-reasoning",
            displayName = "Phi-4 Mini Reasoning",
            provider = ModelProvider.Microsoft,
            parameterCount = "3.8B",
            quantization = "int4",
            contextWindow = 16_384,
            supportedBackends = listOf(InferenceBackend.LiteRtLm),
            minimumMemoryMb = 3_072,
            status = ModelStatus.DownloadRequired,
            runtimeModel = AiModel.Phi4MiniReasoning,
            // Verified ungated CPU .litertlm (~2.8 GB).
            downloadUrl = "https://huggingface.co/litert-community/Phi-4-mini-reasoning/resolve/main/model.litertlm",
        ),
        LocalModel(
            id = "codegemma-7b",
            displayName = "CodeGemma 7B",
            provider = ModelProvider.Google,
            parameterCount = "7B",
            quantization = "int4",
            contextWindow = 8_192,
            supportedBackends = listOf(InferenceBackend.LiteRtLm),
            minimumMemoryMb = 6_144,
            status = ModelStatus.DownloadRequired,
            runtimeModel = AiModel.CodeGemma7B,
            // Verified ungated CPU .litertlm (~4.7 GB). Large — high-end devices only.
            downloadUrl = "https://huggingface.co/litert-community/codegemma-7b-it-int4-litertlm/resolve/main/codegemma-7b-it-int4-litertlm.litertlm",
        ),
        LocalModel(
            id = "gemma-4-12b",
            displayName = "Gemma 4 12B",
            provider = ModelProvider.Google,
            parameterCount = "12B",
            quantization = "int4",
            contextWindow = 8_192,
            supportedBackends = listOf(InferenceBackend.LiteRtLm),
            minimumMemoryMb = 8_192,
            status = ModelStatus.DownloadRequired,
            runtimeModel = AiModel.Gemma4_12B,
            // Verified ungated CPU .litertlm (~6.9 GB). Very large — flagship devices only.
            downloadUrl = "https://huggingface.co/litert-community/gemma-4-12B-it-litert-lm/resolve/main/gemma-4-12B-it.litertlm",
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

    // Only surface models the current platform can actually run: those declaring at least one
    // backend this build provides (see BackendCatalog). Models whose only backends are ONNX or
    // llama.cpp have no real runtime here, so rather than listing them as permanently
    // "unavailable"/"coming soon" they are hidden on platforms that can't use them.
    private val baseModels = allModels.filter { model ->
        model.supportedBackends.any { it in usableBackends }
    }

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
