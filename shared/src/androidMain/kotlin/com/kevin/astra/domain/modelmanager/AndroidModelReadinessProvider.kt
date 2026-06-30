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
                model.status == ModelStatus.DownloadRequired -> model.downloadableReadiness()
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
        // Check filesDir first (downloaded models)
        val filesDirModel = context?.let { ctx ->
            val modelDir = java.io.File(ctx.filesDir, "astra-models/${runtimeModel.filesystemId}")
            if (modelDir.exists()) {
                modelDir.listFiles()?.firstOrNull { f ->
                    f.name.endsWith(".litertlm") || f.name.endsWith(".task") || f.name.endsWith(".tflite")
                }
            } else null
        }
        if (filesDirModel != null) {
            return baseReadiness(
                requiredFiles = listOf(
                    RequiredModelFile(filesDirModel.absolutePath, true, "Downloaded LiteRT-LM model bundle"),
                ),
                localPath = filesDirModel.parent ?: filesDirModel.absolutePath,
                status = ModelReadinessStatus.Installed,
                readinessMessage = "Downloaded model ready. Real on-device inference active when LiteRT-LM is selected.",
            )
        }

        // Fall back to assets (bundled models, e.g. Gemma 3 1B)
        val root = "models/litert-lm"
        val files = context?.assets?.list(root).orEmpty().toSet()
        val bundlePresent = files.any { it.endsWith(".task") || it.endsWith(".litertlm") }
        val splitModelPresent = files.any { it.endsWith(".tflite") || it.endsWith(".bin") }
        val tokenizerPresent = files.any { it.endsWith(".model") || it.endsWith(".spm") || it.contains("tokenizer", ignoreCase = true) }
        val configPresent = files.any { it.endsWith(".json") }
        val hasSupportedBundle = bundlePresent || (splitModelPresent && tokenizerPresent)
        val requiredFiles = listOf(
            RequiredModelFile("$root/gemma.task or $root/gemma.litertlm", bundlePresent, "LiteRT-LM generative model bundle"),
            RequiredModelFile("$root/model.tflite + tokenizer.model", splitModelPresent && tokenizerPresent, "Legacy split model/tokenizer fallback"),
            RequiredModelFile("$root/config.json", configPresent, "Optional runtime configuration"),
        )

        return baseReadiness(
            requiredFiles = requiredFiles,
            localPath = "assets/$root/",
            status = if (hasSupportedBundle) ModelReadinessStatus.Installed else ModelReadinessStatus.ModelRequired,
            readinessMessage = if (hasSupportedBundle) {
                "LiteRT-LM bundle detected in assets. Real on-device inference active when LiteRT-LM is selected."
            } else {
                "No bundle in assets. Download this model or add the bundle manually."
            },
        )
    }

    private fun LocalModel.downloadableReadiness(): ModelReadiness =
        baseReadiness(
            requiredFiles = emptyList(),
            localPath = "Not downloaded",
            status = ModelReadinessStatus.ModelRequired,
            readinessMessage = "Download required. Tap Download in the Model Manager to install on-device.",
        )

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
