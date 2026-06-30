package com.kevin.astra.domain.modelmanager

import kotlinx.coroutines.flow.StateFlow

data class ModelDownloadRequest(
    val modelId: String,
    val displayName: String,
    val url: String,
    val fileName: String,
)

sealed interface ModelDownloadState {
    data object Idle : ModelDownloadState
    data class Downloading(
        val modelId: String,
        val progressPercent: Int,
        val downloadedMb: Float,
        val totalMb: Float,
    ) : ModelDownloadState
    data class Completed(val modelId: String, val localPath: String) : ModelDownloadState
    data class Failed(val modelId: String, val reason: String) : ModelDownloadState
}

interface ModelDownloadManager {
    val downloadState: StateFlow<ModelDownloadState>
    suspend fun download(request: ModelDownloadRequest)
    fun cancel(modelId: String)
    fun deleteModel(modelId: String): Boolean
    fun getInstalledModelPaths(): Map<String, String>
    fun getStorageUsageMb(): Float
}

expect fun createModelDownloadManager(): ModelDownloadManager
