@file:OptIn(ExperimentalForeignApi::class)

package com.kevin.astra.domain.modelmanager

import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.ModelStatus
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun createModelReadinessProvider(): ModelReadinessProvider = IosModelReadinessProvider()

private const val AstraModelsDir = "astra-models"

/**
 * Mirrors `AndroidModelReadinessProvider`: checks the app's Documents directory for a downloaded
 * LiteRT-LM bundle first (see [IosModelDownloadManager]), then falls back to a `.litertlm` file
 * bundled into the iosApp target's resources (added manually via Xcode — see
 * docs/10_iOS_LiteRT_LM_Setup.md).
 */
class IosModelReadinessProvider : ModelReadinessProvider {
    private val fileManager = NSFileManager.defaultManager

    override fun readinessFor(models: List<LocalModel>): List<ModelReadiness> =
        models.map { model ->
            when {
                // Real backends first: a downloaded LiteRT-LM model also flips to
                // ModelStatus.Installed (and is seeded as installed at startup), so it must be
                // reported via liteRtLmReadiness() — which finds the file in Documents, sets
                // isDownloadedToFilesDir=true and enables delete — not as the built-in mock runtime.
                InferenceBackend.LiteRtLm in model.supportedBackends -> model.liteRtLmReadiness()
                InferenceBackend.LiteRt in model.supportedBackends -> model.unsupportedTensorReadiness()
                model.status == ModelStatus.Installed -> model.installedReadiness()
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
        // 1. Documents directory (downloaded via the app's Model Manager)
        val modelDir = "${documentsDirectory()}/$AstraModelsDir/${runtimeModel.filesystemId}"
        val downloadedFile = firstModelFile(modelDir)
        if (downloadedFile != null) {
            return baseReadiness(
                requiredFiles = listOf(
                    RequiredModelFile(downloadedFile, true, "Downloaded LiteRT-LM model"),
                ),
                localPath = modelDir,
                status = ModelReadinessStatus.Installed,
                isDownloadedToFilesDir = true,
                readinessMessage = "Downloaded and ready. Real on-device inference active.",
            )
        }

        // 2. App bundle resource (added manually via Xcode)
        val bundlePath = NSBundle.mainBundle.pathForResource(runtimeModel.filesystemId, ofType = "litertlm")
            ?: NSBundle.mainBundle.pathForResource("gemma", ofType = "litertlm")
        if (bundlePath != null) {
            return baseReadiness(
                requiredFiles = listOf(
                    RequiredModelFile(bundlePath, true, "Bundled in app resources"),
                ),
                localPath = bundlePath,
                status = ModelReadinessStatus.Installed,
                isDownloadedToFilesDir = false,
                readinessMessage = "Bundled with the app — cannot be deleted from Model Manager.",
            )
        }

        // 3. Not available anywhere → offer download when a URL exists
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
                    RequiredModelFile("${runtimeModel.filesystemId}.litertlm", false, "LiteRT-LM model bundle"),
                ),
                localPath = "Not installed",
                status = ModelReadinessStatus.MissingFiles,
                isDownloadedToFilesDir = false,
                readinessMessage = "Add a .litertlm file named '${runtimeModel.filesystemId}' to the iosApp bundle and rebuild.",
            )
        }
    }

    // ── LiteRT (tensor, non-LM) — not implemented on iOS ───────────────────────

    private fun LocalModel.unsupportedTensorReadiness(): ModelReadiness =
        baseReadiness(
            requiredFiles = emptyList(),
            localPath = "N/A",
            status = ModelReadinessStatus.UnsupportedPlatform,
            isDownloadedToFilesDir = false,
            readinessMessage = "Plain LiteRT tensor runtime is Android-only. Use Mock fallback on iOS.",
        )

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

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun firstModelFile(dir: String): String? {
        val contents = fileManager.contentsOfDirectoryAtPath(dir, error = null) as? List<*> ?: return null
        val match = contents.filterIsInstance<String>()
            .firstOrNull { it.endsWith(".litertlm") || it.endsWith(".task") }
            ?: return null
        return "$dir/$match"
    }

    private fun documentsDirectory(): String =
        (NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).firstOrNull() as? String)
            ?: platform.Foundation.NSTemporaryDirectory()

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
