package com.kevin.astra.domain.modelmanager

import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.LocalModel

enum class ModelReadinessStatus(val label: String) {
    Installed("Installed"),
    ModelRequired("Model Required"),
    MissingFiles("Missing Files"),
    UnsupportedPlatform("Unsupported Platform"),
    ComingSoon("Coming Soon"),
}

data class RequiredModelFile(
    val path: String,
    val present: Boolean,
    val description: String,
)

data class ModelReadiness(
    val modelId: String,
    val displayName: String,
    val provider: String,
    val parameterCount: String,
    val quantization: String,
    val expectedSize: String,
    val supportedBackends: List<InferenceBackend>,
    val requiredFiles: List<RequiredModelFile>,
    val localPath: String,
    val status: ModelReadinessStatus,
    val readinessMessage: String,
    // true only when the model file lives in filesDir (downloaded via the app — deletable)
    val isDownloadedToFilesDir: Boolean = false,
)

interface ModelReadinessProvider {
    fun readinessFor(models: List<LocalModel>): List<ModelReadiness>
}

expect fun createModelReadinessProvider(): ModelReadinessProvider

