@file:OptIn(ExperimentalForeignApi::class)

package com.kevin.astra.domain.modelmanager

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSUserDomainMask
import platform.darwin.NSObject

actual fun createModelDownloadManager(): ModelDownloadManager = IosModelDownloadManager()

private const val AstraModelsDir = "astra-models"

/**
 * Real HTTP download over `NSURLSession`, mirroring `AndroidModelDownloadManager`: streams a model
 * into the app's Documents directory with progress reporting, supports cancel/delete, and reports
 * on-device storage usage. Downloaded bundles are picked up by [IosLiteRtLmModelLoader] ahead of
 * any model bundled into the app's resources.
 */
class IosModelDownloadManager : ModelDownloadManager {
    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    override val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    private var activeTask: NSURLSessionDownloadTask? = null
    private var activeDelegate: DownloadDelegate? = null
    private var activeModelId: String? = null

    override suspend fun download(request: ModelDownloadRequest) {
        if (activeModelId == request.modelId) return
        cancel(activeModelId ?: "")

        val url = NSURL.URLWithString(request.url)
        if (url == null) {
            _downloadState.value = ModelDownloadState.Failed(request.modelId, "Invalid download URL.")
            return
        }

        activeModelId = request.modelId
        _downloadState.value = ModelDownloadState.Downloading(request.modelId, 0, 0f, 0f)

        val destDir = modelDirectory(request.modelId)
        NSFileManager.defaultManager.createDirectoryAtPath(
            path = destDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        val destPath = "$destDir/${request.fileName}"

        // Three separate attempts to set the Authorization header directly on an
        // NSMutableURLRequest (`initWithURL:`-style constructor + setValue(forHTTPHeaderField:),
        // the `allHTTPHeaderFields` property, and requestWithURL(_:) + setValue(forHTTPHeaderField:))
        // all failed with "Unresolved reference" from the Kotlin/Native compiler — the header-field
        // API surface isn't resolving on this class in this project's cinterop binding. Sidestepping
        // it entirely: set the header on the session's `HTTPAdditionalHeaders` config dictionary
        // instead, which every request made through that session automatically carries.
        // (`requestWithURL` already types its return as NSMutableURLRequest here — confirmed by
        // Xcode's own "No cast needed" warning on an earlier `as NSMutableURLRequest`.)
        val urlRequest = NSMutableURLRequest.requestWithURL(url)
        val authToken = request.authToken

        val delegate = DownloadDelegate(
            destinationPath = destPath,
            onProgress = { percent, downloadedMb, totalMb ->
                if (activeModelId == request.modelId) {
                    _downloadState.value = ModelDownloadState.Downloading(request.modelId, percent, downloadedMb, totalMb)
                }
            },
            onComplete = { localPath, error ->
                if (activeModelId == request.modelId) {
                    activeModelId = null
                    activeTask = null
                    activeDelegate = null
                    _downloadState.value = if (localPath != null) {
                        ModelDownloadState.Completed(request.modelId, localPath)
                    } else {
                        ModelDownloadState.Failed(request.modelId, error ?: "Download failed")
                    }
                }
            },
            onAuthFailure = { code ->
                if (activeModelId == request.modelId) {
                    activeModelId = null
                    activeTask = null
                    activeDelegate = null
                    _downloadState.value = ModelDownloadState.Failed(
                        request.modelId,
                        "Authentication required (HTTP $code). This model requires a HuggingFace account.",
                    )
                }
            },
        )
        activeDelegate = delegate

        val sessionConfiguration = NSURLSessionConfiguration.defaultSessionConfiguration
        if (authToken != null) {
            sessionConfiguration.HTTPAdditionalHeaders = mapOf("Authorization" to "Bearer $authToken")
        }
        val session = NSURLSession.sessionWithConfiguration(
            configuration = sessionConfiguration,
            delegate = delegate,
            delegateQueue = null,
        )
        val task = session.downloadTaskWithRequest(urlRequest)
        activeTask = task
        task.resume()
    }

    override fun cancel(modelId: String) {
        if (activeModelId != modelId) return
        activeTask?.cancel()
        activeTask = null
        activeDelegate = null
        activeModelId = null
        _downloadState.value = ModelDownloadState.Idle
    }

    override fun deleteModel(modelId: String): Boolean =
        NSFileManager.defaultManager.removeItemAtPath(modelDirectory(modelId), error = null)

    override fun getInstalledModelPaths(): Map<String, String> {
        val fileManager = NSFileManager.defaultManager
        val root = rootModelsDirectory()
        val modelIds = (fileManager.contentsOfDirectoryAtPath(root, error = null) as? List<*>)
            ?.filterIsInstance<String>()
            ?: return emptyMap()

        return modelIds.mapNotNull { modelId ->
            val dir = "$root/$modelId"
            val files = (fileManager.contentsOfDirectoryAtPath(dir, error = null) as? List<*>)
                ?.filterIsInstance<String>()
                ?: return@mapNotNull null
            val modelFile = files.firstOrNull {
                it.endsWith(".litertlm") || it.endsWith(".task") || it.endsWith(".tflite")
            } ?: return@mapNotNull null
            modelId to "$dir/$modelFile"
        }.toMap()
    }

    override fun getStorageUsageMb(): Float {
        val fileManager = NSFileManager.defaultManager
        val root = rootModelsDirectory()
        val modelIds = (fileManager.contentsOfDirectoryAtPath(root, error = null) as? List<*>)
            ?.filterIsInstance<String>()
            ?: return 0f

        var totalBytes = 0L
        modelIds.forEach { modelId ->
            val dir = "$root/$modelId"
            val files = (fileManager.contentsOfDirectoryAtPath(dir, error = null) as? List<*>)
                ?.filterIsInstance<String>()
                ?: return@forEach
            files.forEach { file ->
                val attrs = fileManager.attributesOfItemAtPath("$dir/$file", error = null)
                totalBytes += when (val size = attrs?.get(NSFileSize)) {
                    is Long -> size
                    is Int -> size.toLong()
                    is NSNumber -> size.longLongValue
                    else -> 0L
                }
            }
        }
        return totalBytes / (1024f * 1024f)
    }

    private fun rootModelsDirectory(): String = "${documentsDirectory()}/$AstraModelsDir"
    private fun modelDirectory(modelId: String): String = "${rootModelsDirectory()}/$modelId"
}

private fun documentsDirectory(): String =
    (NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).firstOrNull() as? String)
        ?: NSTemporaryDirectory()

/**
 * `NSURLSessionDownloadDelegate` implementation backing the download. Reports progress via
 * [onProgress], and either [onComplete] (success/failure) or [onAuthFailure] (401/403 — mirrors
 * the HuggingFace gated-model case already handled on Android) exactly once per download.
 *
 * Any non-success HTTP status (>= 400) is rejected rather than treated as a completed download:
 * NSURLSession still routes error responses (404, 500, …) to `didFinishDownloadingToURL` with the
 * error page as the "downloaded" body, so without this guard that HTML/error content would be
 * moved into the model directory and reported as installed.
 */
private class DownloadDelegate(
    private val destinationPath: String,
    private val onProgress: (Int, Float, Float) -> Unit,
    private val onComplete: (String?, String?) -> Unit,
    private val onAuthFailure: (Long) -> Unit,
) : NSObject(), NSURLSessionDownloadDelegateProtocol {

    /**
     * Routes a non-success HTTP status to the right terminal callback and returns true when it did
     * (so the caller stops). 401/403 surface as an auth failure; every other >= 400 status becomes
     * a plain download failure. Returns false for a missing status or any 2xx/3xx.
     */
    private fun rejectIfHttpError(statusCode: Long?): Boolean {
        if (statusCode == null || statusCode < 400L) return false
        when (statusCode) {
            401L, 403L -> onAuthFailure(statusCode)
            else -> onComplete(null, "Download failed: the server returned HTTP $statusCode.")
        }
        return true
    }

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didWriteData: Long,
        totalBytesWritten: Long,
        totalBytesExpectedToWrite: Long,
    ) {
        if (rejectIfHttpError((downloadTask.response as? platform.Foundation.NSHTTPURLResponse)?.statusCode)) {
            downloadTask.cancel()
            return
        }
        val percent = if (totalBytesExpectedToWrite > 0) {
            ((totalBytesWritten * 100) / totalBytesExpectedToWrite).toInt()
        } else {
            0
        }
        val downloadedMb = totalBytesWritten / (1024f * 1024f)
        val totalMb = if (totalBytesExpectedToWrite > 0) totalBytesExpectedToWrite / (1024f * 1024f) else 0f
        onProgress(percent, downloadedMb, totalMb)
    }

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didFinishDownloadingToURL: NSURL,
    ) {
        if (rejectIfHttpError((downloadTask.response as? platform.Foundation.NSHTTPURLResponse)?.statusCode)) {
            return
        }
        val sourcePath = didFinishDownloadingToURL.path
        if (sourcePath == null) {
            onComplete(null, "Downloaded file has no path.")
            return
        }
        val fileManager = NSFileManager.defaultManager
        fileManager.removeItemAtPath(destinationPath, error = null)
        val moved = fileManager.moveItemAtPath(sourcePath, destinationPath, error = null)
        if (moved) {
            onComplete(destinationPath, null)
        } else {
            onComplete(null, "Could not move downloaded file into place.")
        }
    }

    override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) {
        if (didCompleteWithError != null) {
            onComplete(null, didCompleteWithError.localizedDescription)
        }
    }
}
