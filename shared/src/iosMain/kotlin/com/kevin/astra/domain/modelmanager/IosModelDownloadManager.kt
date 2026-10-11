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
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSUserDomainMask
import platform.Foundation.setValue
import platform.darwin.NSObject

actual fun createModelDownloadManager(): ModelDownloadManager = IosModelDownloadManager.shared

private const val AstraModelsDir = "astra-models"
private const val BackgroundSessionId = "com.kevin.astra.model-download"

/**
 * Downloads models with a *background* `NSURLSession`: the transfer is run by the system
 * (nsurlsessiond), so it keeps going while the app is suspended and even after iOS terminates
 * it. A default session stopped as soon as the user left the app.
 *
 * Background sessions are per-identifier singletons, so there is one session and one delegate
 * for the process. Each task carries "modelId/fileName" in `taskDescription`, which lets the
 * delegate file a download that finishes after a relaunch, when no in-memory state survives.
 */
class IosModelDownloadManager private constructor() : ModelDownloadManager {
    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    override val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    private var activeTask: NSURLSessionDownloadTask? = null
    private var activeModelId: String? = null

    private val delegate = BackgroundDownloadDelegate(
        destinationFor = { description -> destinationFor(description) },
        onProgress = { modelId, percent, downloadedMb, totalMb ->
            if (activeModelId == null || activeModelId == modelId) {
                activeModelId = modelId
                _downloadState.value = ModelDownloadState.Downloading(modelId, percent, downloadedMb, totalMb)
            }
        },
        onFinished = { modelId, localPath, error -> finish(modelId, localPath, error) },
        onAllEventsDelivered = { BackgroundDownloadEvents.complete() },
    )

    private val session: NSURLSession by lazy {
        val configuration = NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(BackgroundSessionId)
            .apply {
                sessionSendsLaunchEvents = true // relaunch the app in the background to file the model
                discretionary = false // start now, not when iOS finds it convenient
                allowsCellularAccess = true
            }
        NSURLSession.sessionWithConfiguration(configuration, delegate, NSOperationQueue.mainQueue)
    }

    init {
        // Re-attach to a transfer started before the app was suspended or relaunched.
        session.getAllTasksWithCompletionHandler { tasks ->
            val running = tasks?.filterIsInstance<NSURLSessionDownloadTask>()?.firstOrNull()
            val modelId = running?.taskDescription?.substringBefore('/')
            if (running != null && modelId != null) {
                activeTask = running
                activeModelId = modelId
                _downloadState.value = ModelDownloadState.Downloading(modelId, 0, 0f, 0f)
            }
        }
    }

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

        val urlRequest = NSMutableURLRequest.requestWithURL(url)
        request.authToken?.let { urlRequest.setValue("Bearer $it", forHTTPHeaderField = "Authorization") }
        val task = session.downloadTaskWithRequest(urlRequest)
        task.taskDescription = "${request.modelId}/${request.fileName}"
        activeTask = task
        task.resume()
    }

    override fun cancel(modelId: String) {
        if (activeModelId != modelId) return
        activeTask?.cancel()
        activeTask = null
        activeModelId = null
        _downloadState.value = ModelDownloadState.Idle
    }

    private fun finish(modelId: String, localPath: String?, error: String?) {
        if (activeModelId != null && activeModelId != modelId) return
        // A cancelled task reports an error after cancel() already reset the state.
        if (activeModelId == null && localPath == null) return
        activeModelId = null
        activeTask = null
        _downloadState.value = if (localPath != null) {
            ModelDownloadState.Completed(modelId, localPath)
        } else {
            ModelDownloadState.Failed(modelId, error ?: "Download failed")
        }
    }

    /** "modelId/fileName" → (modelId, absolute destination path), creating the model directory. */
    private fun destinationFor(description: String?): Pair<String, String>? {
        val modelId = description?.substringBefore('/', missingDelimiterValue = "")?.takeIf { it.isNotEmpty() }
            ?: return null
        val fileName = description.substringAfter('/').takeIf { it.isNotEmpty() } ?: return null
        val dir = modelDirectory(modelId)
        NSFileManager.defaultManager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
        return modelId to "$dir/$fileName"
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

    companion object {
        /** One instance per process: it owns the background session for [BackgroundSessionId]. */
        val shared: IosModelDownloadManager by lazy { IosModelDownloadManager() }
    }
}

/**
 * Bridge for `application(_:handleEventsForBackgroundURLSession:completionHandler:)`.
 * When iOS relaunches the app to deliver finished transfers, Swift hands the completion
 * handler here; it is called once the session has delivered every event.
 */
object BackgroundDownloadEvents {
    private var completionHandler: (() -> Unit)? = null

    fun handle(identifier: String, completionHandler: () -> Unit) {
        if (identifier != BackgroundSessionId) return completionHandler()
        this.completionHandler = completionHandler
        IosModelDownloadManager.shared // recreate the session so it can deliver the events
    }

    internal fun complete() {
        completionHandler?.invoke()
        completionHandler = null
    }
}

private fun documentsDirectory(): String =
    (NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).firstOrNull() as? String)
        ?: NSTemporaryDirectory()

/**
 * Delegate of the background session. Error responses (401/403/404/5xx) are rejected rather than
 * filed as models: NSURLSession still routes them to `didFinishDownloadingToURL` with the error
 * page as the "downloaded" body.
 */
private class BackgroundDownloadDelegate(
    private val destinationFor: (String?) -> Pair<String, String>?,
    private val onProgress: (modelId: String, percent: Int, downloadedMb: Float, totalMb: Float) -> Unit,
    private val onFinished: (modelId: String, localPath: String?, error: String?) -> Unit,
    private val onAllEventsDelivered: () -> Unit,
) : NSObject(), NSURLSessionDownloadDelegateProtocol {
    /** Tasks already reported as finished, so didCompleteWithError doesn't report them twice. */
    private val reported = mutableSetOf<ULong>()

    private fun modelIdOf(task: NSURLSessionTask): String? = task.taskDescription?.substringBefore('/')

    private fun httpError(task: NSURLSessionTask): String? {
        val code = (task.response as? NSHTTPURLResponse)?.statusCode ?: return null
        return when {
            code == 401L || code == 403L -> "Authentication required (HTTP $code). This model requires a HuggingFace account."
            code >= 400L -> "Download failed: the server returned HTTP $code."
            else -> null
        }
    }

    private fun report(task: NSURLSessionTask, localPath: String?, error: String?) {
        val modelId = modelIdOf(task) ?: return
        if (!reported.add(task.taskIdentifier)) return
        onFinished(modelId, localPath, error)
    }

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didWriteData: Long,
        totalBytesWritten: Long,
        totalBytesExpectedToWrite: Long,
    ) {
        httpError(downloadTask)?.let { error ->
            report(downloadTask, null, error)
            downloadTask.cancel()
            return
        }
        val modelId = modelIdOf(downloadTask) ?: return
        val percent = if (totalBytesExpectedToWrite > 0) ((totalBytesWritten * 100) / totalBytesExpectedToWrite).toInt() else 0
        val totalMb = if (totalBytesExpectedToWrite > 0) totalBytesExpectedToWrite / (1024f * 1024f) else 0f
        onProgress(modelId, percent, totalBytesWritten / (1024f * 1024f), totalMb)
    }

    override fun URLSession(
        session: NSURLSession,
        downloadTask: NSURLSessionDownloadTask,
        didFinishDownloadingToURL: NSURL,
    ) {
        httpError(downloadTask)?.let { return report(downloadTask, null, it) }
        // The temp file is deleted when this method returns: move it synchronously.
        val (_, destinationPath) = destinationFor(downloadTask.taskDescription)
            ?: return report(downloadTask, null, "Unknown download destination.")
        val sourcePath = didFinishDownloadingToURL.path
            ?: return report(downloadTask, null, "Downloaded file has no path.")
        val fileManager = NSFileManager.defaultManager
        fileManager.removeItemAtPath(destinationPath, error = null)
        if (fileManager.moveItemAtPath(sourcePath, destinationPath, error = null)) {
            report(downloadTask, destinationPath, null)
        } else {
            report(downloadTask, null, "Could not move downloaded file into place.")
        }
    }

    override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) {
        if (didCompleteWithError != null) report(task, null, didCompleteWithError.localizedDescription)
    }

    override fun URLSessionDidFinishEventsForBackgroundURLSession(session: NSURLSession) {
        onAllEventsDelivered()
    }
}
