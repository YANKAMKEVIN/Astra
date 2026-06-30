package com.kevin.astra.domain.modelmanager

import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.ModelStatus

class StaticModelReadinessProvider(
    private val platformName: String,
    private val supportsLiteRtLmAssets: Boolean = false,
) : ModelReadinessProvider {
    override fun readinessFor(models: List<LocalModel>): List<ModelReadiness> =
        models.map { model ->
            val status = when {
                model.status == ModelStatus.Installed -> ModelReadinessStatus.Installed
                InferenceBackend.LiteRtLm in model.supportedBackends && !supportsLiteRtLmAssets -> ModelReadinessStatus.UnsupportedPlatform
                model.status == ModelStatus.Unsupported -> ModelReadinessStatus.UnsupportedPlatform
                else -> ModelReadinessStatus.ComingSoon
            }
            ModelReadiness(
                modelId = model.id,
                displayName = model.displayName,
                provider = model.provider.label,
                parameterCount = model.parameterCount,
                quantization = model.quantization,
                expectedSize = expectedSizeFor(model.id),
                supportedBackends = model.supportedBackends,
                requiredFiles = emptyList(),
                localPath = if (status == ModelReadinessStatus.Installed) {
                    "Built-in mock runtime"
                } else {
                    "N/A"
                },
                status = status,
                readinessMessage = when (status) {
                    ModelReadinessStatus.Installed -> "Ready through the installed Mock fallback model."
                    ModelReadinessStatus.UnsupportedPlatform -> "Not supported on $platformName yet. Use Mock fallback."
                    ModelReadinessStatus.ComingSoon -> "Model download and local bundle management are coming soon."
                    ModelReadinessStatus.ModelRequired,
                    ModelReadinessStatus.MissingFiles,
                    -> "Local model files are required before this runtime can execute."
                },
            )
        }
}

fun expectedSizeFor(modelId: String): String =
    when (modelId) {
        "mock-model" -> "Built-in"
        "gemma-3-1b" -> "~0.8 GB"
        "gemma-3-4b" -> "~2.5 GB"
        "phi-4-mini" -> "~2.2 GB"
        "phi-3-mini" -> "~2–4 GB"
        "qwen3-1-7b" -> "~1.1 GB"
        "qwen-2-5-1-5b" -> "~1–3 GB"
        "smollm2-360m" -> "~230 MB"
        "llama-3-2-1b" -> "~0.7 GB"
        "llama-3-2-3b" -> "~2–4 GB"
        else -> "Unknown"
    }
