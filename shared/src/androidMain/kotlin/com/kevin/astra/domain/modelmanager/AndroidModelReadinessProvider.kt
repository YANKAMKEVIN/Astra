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
                // Mock model is always installed by definition — no file check needed
                model.status == ModelStatus.Installed -> model.installedReadiness()
                InferenceBackend.LiteRtLm in model.supportedBackends -> model.liteRtLmReadiness()
                InferenceBackend.LiteRt in model.supportedBackends -> model.liteRtReadiness()
                model.status == ModelStatus.DownloadRequired -> model.downloadableReadiness()
                else -> model.comingSoonReadiness()
            }
        }

    // ── Mock / built-in ───────────────────────────────────────────────────────

    private fun LocalModel.installedReadiness(): ModelReadiness =
        baseReadiness(
            requiredFiles = emptyList(),
            localPath = "Built-in mock runtime",
            status = ModelReadinessStatus.Installed,
            isDownloadedToFilesDir = false,
            readinessMessage = "Built-in mock engine — no model file required.",
        )

    // ── LiteRT-LM ─────────────────────────────────────────────────────────────

    private fun LocalModel.liteRtLmReadiness(): ModelReadiness {
        // 1. Check filesDir (downloaded via the app)
        val downloadedFile = context?.let { ctx ->
            val modelDir = java.io.File(ctx.filesDir, "astra-models/${runtimeModel.filesystemId}")
            if (modelDir.exists()) {
                modelDir.listFiles()?.firstOrNull { f ->
                    f.name.endsWith(".litertlm") || f.name.endsWith(".task") || f.name.endsWith(".tflite")
                }
            } else null
        }
        if (downloadedFile != null) {
            return baseReadiness(
                requiredFiles = listOf(
                    RequiredModelFile(downloadedFile.absolutePath, true, "Downloaded LiteRT-LM model"),
                ),
                localPath = downloadedFile.parent ?: downloadedFile.absolutePath,
                status = ModelReadinessStatus.Installed,
                isDownloadedToFilesDir = true,
                readinessMessage = "Downloaded and ready. Real on-device inference active.",
            )
        }

        // 2. Check assets — model-specific subdirectory assets/models/litert-lm/{filesystemId}/
        val assetDir = "models/litert-lm/${runtimeModel.filesystemId}"
        val assetFiles = runCatching {
            context?.assets?.list(assetDir).orEmpty().toSet()
        }.getOrDefault(emptySet())

        val bundlePresent = assetFiles.any { it.endsWith(".task") || it.endsWith(".litertlm") }
        val tflitePresent = assetFiles.any { it.endsWith(".tflite") }
        val tokenizerPresent = assetFiles.any {
            it.endsWith(".model") || it.endsWith(".spm") || it.contains("tokenizer", ignoreCase = true)
        }
        val hasSupportedBundle = bundlePresent || (tflitePresent && tokenizerPresent)

        if (hasSupportedBundle) {
            return baseReadiness(
                requiredFiles = listOf(
                    RequiredModelFile("assets/$assetDir/", true, "Bundled in app assets"),
                ),
                localPath = "assets/$assetDir/",
                status = ModelReadinessStatus.Installed,
                isDownloadedToFilesDir = false,
                readinessMessage = "Bundled in app assets. Deployed with the APK — cannot be deleted.",
            )
        }

        // 3. Not available anywhere → show download option if URL exists, else require manual setup
        return if (downloadUrl != null) {
            baseReadiness(
                requiredFiles = emptyList(),
                localPath = "Not downloaded",
                status = ModelReadinessStatus.ModelRequired,
                isDownloadedToFilesDir = false,
                readinessMessage = "Not installed. Tap \"Download\" to fetch from HuggingFace (~${expectedSizeFor(id)}).",
            )
        } else {
            baseReadiness(
                requiredFiles = listOf(
                    RequiredModelFile("assets/$assetDir/*.litertlm", false, "LiteRT-LM model bundle"),
                ),
                localPath = "assets/$assetDir/",
                status = ModelReadinessStatus.MissingFiles,
                isDownloadedToFilesDir = false,
                readinessMessage = "Add the model bundle to assets/$assetDir/ and rebuild the app.",
            )
        }
    }

    // ── LiteRT (tensor) ───────────────────────────────────────────────────────

    private fun LocalModel.liteRtReadiness(): ModelReadiness {
        val path = "models/astra-slm.tflite"
        val present = runCatching {
            context?.assets?.open(path)?.use { true } == true
        }.getOrDefault(false)
        return baseReadiness(
            requiredFiles = listOf(
                RequiredModelFile(path, present, "LiteRT tensor model"),
            ),
            localPath = "assets/$path",
            status = if (present) ModelReadinessStatus.Installed else ModelReadinessStatus.MissingFiles,
            isDownloadedToFilesDir = false,
            readinessMessage = if (present) {
                "LiteRT tensor model found in assets."
            } else {
                "Missing assets/$path. Add the model file and rebuild."
            },
        )
    }

    // ── Downloadable (no backend yet) ─────────────────────────────────────────

    private fun LocalModel.downloadableReadiness(): ModelReadiness =
        baseReadiness(
            requiredFiles = emptyList(),
            localPath = "Not downloaded",
            status = ModelReadinessStatus.ModelRequired,
            isDownloadedToFilesDir = false,
            readinessMessage = "Not installed. Tap \"Download\" to install on-device.",
        )

    // ── Coming soon ───────────────────────────────────────────────────────────

    private fun LocalModel.comingSoonReadiness(): ModelReadiness =
        baseReadiness(
            requiredFiles = emptyList(),
            localPath = "N/A",
            status = ModelReadinessStatus.ComingSoon,
            isDownloadedToFilesDir = false,
            readinessMessage = "Runtime support coming soon. Use Mock fallback for now.",
        )

    // ── Base builder ──────────────────────────────────────────────────────────

    private fun LocalModel.baseReadiness(
        requiredFiles: List<RequiredModelFile>,
        localPath: String,
        status: ModelReadinessStatus,
        isDownloadedToFilesDir: Boolean,
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
            isDownloadedToFilesDir = isDownloadedToFilesDir,
            readinessMessage = readinessMessage,
        )
}
