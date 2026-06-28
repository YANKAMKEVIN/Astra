package com.kevin.astra.domain.modelmanager

import android.content.Context
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.ModelStatus

private var modelReadinessContext: Context? = null

fun initializeAndroidModelReadinessProvider(context: Context) {
    modelReadinessContext = context.applicationContext
}

actual fun createModelReadinessProvider(): ModelReadinessProvider =
    AndroidModelReadinessProvider(context = modelReadinessContext)

class AndroidModelReadinessProvider(
    private val context: Context?,
) : ModelReadinessProvider {
    override fun readinessFor(models: List<LocalModel>): List<ModelReadiness> =
        models.map { model ->
            when {
                model.status == ModelStatus.Installed -> model.installedReadiness()
                InferenceBackend.LiteRtLm in model.supportedBackends -> model.liteRtLmReadiness()
                InferenceBackend.LiteRt in model.supportedBackends -> model.liteRtReadiness()
                else -> model.comingSoonReadiness()
            }
        }

    private fun LocalModel.installedReadiness(): ModelReadiness =
        baseReadiness(
            requiredFiles = emptyList(),
            localPath = "Built-in mock runtime",
            status = ModelReadinessStatus.Installed,
            readinessMessage = "Installed and ready for deterministic local fallback.",
        )

    private fun LocalModel.liteRtLmReadiness(): ModelReadiness {
        val root = "models/litert-lm"
        val files = context?.assets?.list(root).orEmpty().toSet()
        val modelPresent = files.any { it.endsWith(".tflite") || it.endsWith(".task") || it.endsWith(".bin") }
        val tokenizerPresent = files.any { it.endsWith(".model") || it.endsWith(".spm") || it.contains("tokenizer", ignoreCase = true) }
        val configPresent = files.any { it.endsWith(".json") }
        val requiredFiles = listOf(
            RequiredModelFile("$root/model.tflite", modelPresent, "LiteRT-LM model file"),
            RequiredModelFile("$root/tokenizer.model", tokenizerPresent, "Tokenizer model"),
            RequiredModelFile("$root/config.json", configPresent, "Optional runtime configuration"),
        )
        val missingRequired = requiredFiles.filter { !it.present && !it.description.startsWith("Optional") }

        return baseReadiness(
            requiredFiles = requiredFiles,
            localPath = "shared/src/androidMain/assets/$root/",
            status = if (missingRequired.isEmpty()) ModelReadinessStatus.ModelRequired else ModelReadinessStatus.MissingFiles,
            readinessMessage = if (missingRequired.isEmpty()) {
                "LiteRT-LM bundle detected. Generative session integration is prepared, but full generation remains staged."
            } else {
                "Missing required files: ${missingRequired.joinToString { it.description }}. Use Mock fallback until the bundle is added."
            },
        )
    }

    private fun LocalModel.liteRtReadiness(): ModelReadiness {
        val path = "models/astra-slm.tflite"
        val present = runCatching {
            context?.assets?.open(path)?.use { true } == true
        }.getOrDefault(false)
        return baseReadiness(
            requiredFiles = listOf(
                RequiredModelFile(path, present, "LiteRT tensor validation model"),
            ),
            localPath = "shared/src/androidMain/assets/$path",
            status = if (present) ModelReadinessStatus.ModelRequired else ModelReadinessStatus.MissingFiles,
            readinessMessage = if (present) {
                "LiteRT tensor model detected and ready for runtime validation."
            } else {
                "Missing $path. See docs/REAL_INFERENCE_SETUP.md or use Mock fallback."
            },
        )
    }

    private fun LocalModel.comingSoonReadiness(): ModelReadiness =
        baseReadiness(
            requiredFiles = emptyList(),
            localPath = "N/A",
            status = ModelReadinessStatus.ComingSoon,
            readinessMessage = "Local file management for this runtime is coming soon. Use Mock fallback.",
        )

    private fun LocalModel.baseReadiness(
        requiredFiles: List<RequiredModelFile>,
        localPath: String,
        status: ModelReadinessStatus,
        readinessMessage: String,
    ): ModelReadiness =
        ModelReadiness(
            modelId = id,
            displayName = displayName,
            provider = provider.label,
            parameterCount = parameterCount,
            quantization = quantization,
            expectedSize = expectedSizeFor(id),
            supportedBackends = supportedBackends,
            requiredFiles = requiredFiles,
            localPath = localPath,
            status = status,
            readinessMessage = readinessMessage,
        )
}

