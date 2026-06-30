package com.kevin.astra.domain.modelmanager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual fun createModelDownloadManager(): ModelDownloadManager = IosModelDownloadManager()

class IosModelDownloadManager : ModelDownloadManager {
    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    override val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    override suspend fun download(request: ModelDownloadRequest) {
        _downloadState.value = ModelDownloadState.Failed(
            modelId = request.modelId,
            reason = "Model download is not yet supported on iOS. Add the model bundle manually via Xcode.",
        )
    }

    override fun cancel(modelId: String) = Unit
    override fun deleteModel(modelId: String): Boolean = false
    override fun getInstalledModelPaths(): Map<String, String> = emptyMap()
    override fun getStorageUsageMb(): Float = 0f
}
