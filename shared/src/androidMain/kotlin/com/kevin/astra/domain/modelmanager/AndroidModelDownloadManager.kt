package com.kevin.astra.domain.modelmanager

import android.content.Context
import com.kevin.astra.app.di.androidAppContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection

actual fun createModelDownloadManager(): ModelDownloadManager {
    return AndroidModelDownloadManager(androidAppContext())
}

class AndroidModelDownloadManager(
    private val context: Context,
) : ModelDownloadManager {
    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    override val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var activeJob: Job? = null
    private var activeModelId: String? = null
    private val downloader = ResumableDownloader()

    override suspend fun download(request: ModelDownloadRequest) {
        if (activeModelId == request.modelId) return
        cancel(activeModelId ?: "")

        activeModelId = request.modelId
        _downloadState.value = ModelDownloadState.Downloading(request.modelId, 0, 0f, 0f)

        // Foreground service: keeps the network and CPU alive when the screen locks.
        ModelDownloadService.start(context, request.modelId, request.displayName)

        activeJob = scope.launch {
            val destDir = File(context.filesDir, "astra-models/${request.modelId}").apply { mkdirs() }
            val destFile = File(destDir, request.fileName)
            val tmpFile = File(destDir, "${request.fileName}.tmp")
            try {
                val result = downloader.download(
                    url = request.url,
                    authToken = request.authToken,
                    partFile = tmpFile,
                ) { downloadedBytes, totalBytes ->
                    _downloadState.value = ModelDownloadState.Downloading(
                        modelId = request.modelId,
                        progressPercent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0,
                        downloadedMb = downloadedBytes / (1024f * 1024f),
                        totalMb = if (totalBytes > 0) totalBytes / (1024f * 1024f) else 0f,
                    )
                }
                when (result) {
                    ResumableDownloader.Result.Completed -> {
                        tmpFile.renameTo(destFile)
                        _downloadState.value = ModelDownloadState.Completed(request.modelId, destFile.absolutePath)
                    }
                    is ResumableDownloader.Result.HttpError -> {
                        tmpFile.delete()
                        _downloadState.value = ModelDownloadState.Failed(request.modelId, httpErrorReason(result.code))
                    }
                }
                activeModelId = null
            } catch (e: CancellationException) {
                tmpFile.delete()
                _downloadState.value = ModelDownloadState.Idle
                throw e
            } catch (e: Exception) {
                // Keep the partial file: tapping Download again resumes where it stopped.
                _downloadState.value = ModelDownloadState.Failed(
                    modelId = request.modelId,
                    reason = "${e.message ?: "Download failed"}. Tap download to resume.",
                )
                activeModelId = null
            }
        }
        activeJob?.join()
    }

    private fun httpErrorReason(code: Int): String = when (code) {
        HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN ->
            "Authentication required (HTTP $code). This model requires a HuggingFace account."
        else -> "Server returned HTTP $code. Check the download URL."
    }

    override fun cancel(modelId: String) {
        if (activeModelId != modelId) return
        activeJob?.cancel()
        activeJob = null
        activeModelId = null
        _downloadState.value = ModelDownloadState.Idle
    }

    override fun deleteModel(modelId: String): Boolean {
        val dir = File(context.filesDir, "astra-models/$modelId")
        return dir.exists() && dir.deleteRecursively()
    }

    override fun getInstalledModelPaths(): Map<String, String> {
        val root = File(context.filesDir, "astra-models")
        if (!root.exists()) return emptyMap()
        return root.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { dir ->
                val modelFile = dir.listFiles()?.firstOrNull { file ->
                    file.name.endsWith(".litertlm") || file.name.endsWith(".task") || file.name.endsWith(".tflite")
                }
                if (modelFile != null) dir.name to modelFile.absolutePath else null
            }
            ?.toMap()
            ?: emptyMap()
    }

    override fun getStorageUsageMb(): Float {
        val root = File(context.filesDir, "astra-models")
        if (!root.exists()) return 0f
        return root.walkBottomUp()
            .filter { it.isFile }
            .sumOf { it.length() }
            .let { it / (1024f * 1024f) }
    }
}

class UnavailableModelDownloadManager(
    private val reason: String,
) : ModelDownloadManager {
    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    override val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    override suspend fun download(request: ModelDownloadRequest) {
        _downloadState.value = ModelDownloadState.Failed(request.modelId, reason)
    }

    override fun cancel(modelId: String) = Unit
    override fun deleteModel(modelId: String): Boolean = false
    override fun getInstalledModelPaths(): Map<String, String> = emptyMap()
    override fun getStorageUsageMb(): Float = 0f
}
