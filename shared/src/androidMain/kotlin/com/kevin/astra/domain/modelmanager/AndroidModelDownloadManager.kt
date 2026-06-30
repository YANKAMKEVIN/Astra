package com.kevin.astra.domain.modelmanager

import android.content.Context
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
import java.net.URL

private var downloadManagerContext: Context? = null

fun initializeAndroidModelDownloadManager(context: Context) {
    downloadManagerContext = context.applicationContext
}

actual fun createModelDownloadManager(): ModelDownloadManager {
    val context = downloadManagerContext
    return if (context != null) {
        AndroidModelDownloadManager(context)
    } else {
        UnavailableModelDownloadManager("Android context is not initialized yet.")
    }
}

class AndroidModelDownloadManager(
    private val context: Context,
) : ModelDownloadManager {
    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    override val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var activeJob: Job? = null
    private var activeModelId: String? = null

    override suspend fun download(request: ModelDownloadRequest) {
        if (activeModelId == request.modelId) return
        cancel(activeModelId ?: "")

        activeModelId = request.modelId
        _downloadState.value = ModelDownloadState.Downloading(request.modelId, 0, 0f, 0f)

        activeJob = scope.launch {
            val destDir = File(context.filesDir, "astra-models/${request.modelId}").apply { mkdirs() }
            val destFile = File(destDir, request.fileName)
            val tmpFile = File(destDir, "${request.fileName}.tmp")
            try {
                val connection = URL(request.url).openConnection() as HttpURLConnection
                connection.connectTimeout = 15_000
                connection.readTimeout = 60_000
                connection.connect()

                val totalBytes = connection.contentLengthLong
                var downloadedBytes = 0L

                connection.inputStream.buffered(65_536).use { input ->
                    tmpFile.outputStream().use { output ->
                        val buffer = ByteArray(65_536)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            val progress = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0
                            _downloadState.value = ModelDownloadState.Downloading(
                                modelId = request.modelId,
                                progressPercent = progress,
                                downloadedMb = downloadedBytes / (1024f * 1024f),
                                totalMb = if (totalBytes > 0) totalBytes / (1024f * 1024f) else 0f,
                            )
                        }
                    }
                }

                tmpFile.renameTo(destFile)
                _downloadState.value = ModelDownloadState.Completed(request.modelId, destFile.absolutePath)
                activeModelId = null
            } catch (e: CancellationException) {
                tmpFile.delete()
                _downloadState.value = ModelDownloadState.Idle
                throw e
            } catch (e: Exception) {
                tmpFile.delete()
                _downloadState.value = ModelDownloadState.Failed(
                    modelId = request.modelId,
                    reason = e.message ?: "Download failed",
                )
                activeModelId = null
            }
        }
        activeJob?.join()
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
