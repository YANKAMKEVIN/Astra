@file:OptIn(ExperimentalForeignApi::class)

package com.kevin.astra.core.ai

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Result of a native LiteRT-LM generation call, produced by the Swift bridge in
 * `iosApp/iosApp/LiteRtLmBridge.swift`, which wraps Google's LiteRT-LM Swift API
 * (`Engine` / `Conversation` — https://ai.google.dev/edge/litert-lm/swift).
 *
 * Exposed to Swift as a plain class with a matching memberwise initializer.
 */
data class NativeLiteRtLmResult(
    val text: String?,
    val errorMessage: String?,
    val latencyMillis: Long,
    val tokensGenerated: Int,
)

/**
 * Kotlin-side contract implemented in Swift. Kotlin/Native exports this interface as an
 * Objective-C/Swift protocol of the same name in the generated `Shared` framework header; the
 * Swift class `LiteRtLmBridge` conforms to it directly and is registered via
 * [registerNativeLiteRtLmEngine] from `ContentView.swift`, before the first prompt is sent.
 *
 * This is a plain callback (not a Kotlin `suspend fun`) on purpose: a closure crosses the
 * Kotlin/Native <-> Swift boundary as an ordinary block, which is far simpler for the Swift side
 * to implement than a suspend-function continuation. The Swift implementation launches its own
 * `Task` internally to call LiteRT-LM's `async` APIs and invokes [completion] when done.
 */
interface NativeLiteRtLmEngine {
    fun generate(
        prompt: String,
        modelPath: String,
        maxTokens: Int,
        completion: (NativeLiteRtLmResult) -> Unit,
    )
}

/**
 * Holds the Swift-provided engine instance. Null until [registerNativeLiteRtLmEngine] runs.
 *
 * `internal set` (not `private set`): [registerNativeLiteRtLmEngine] is a top-level function in
 * this same file, not a member of this object, so a `private` setter (visible only within the
 * object's own body) would not be accessible to it — `internal` opens it up to the whole module.
 */
object LiteRtLmBridge {
    var engine: NativeLiteRtLmEngine? = null
        internal set
}

/**
 * Called from Swift at app startup (`ContentView.swift`), before `MainViewControllerKt.MainViewController()`
 * is invoked, so the bridge is ready before any screen can trigger a generation request.
 */
fun registerNativeLiteRtLmEngine(engine: NativeLiteRtLmEngine) {
    LiteRtLmBridge.engine = engine
}

private const val AstraModelsDir = "astra-models"

/**
 * Locates a LiteRT-LM model bundle for iOS: a model downloaded into the app's Documents directory
 * takes priority (see [IosModelDownloadManager]); otherwise falls back to a `.litertlm` file added
 * manually to the iosApp bundle via Xcode (see docs/10_iOS_LiteRT_LM_Setup.md).
 */
class IosLiteRtLmModelLoader : LiteRtLmModelLoader {
    override suspend fun loadModel(request: PromptRequest): LiteRtLmModelLoadResult {
        val fileManager = NSFileManager.defaultManager
        val modelDir = "${documentsDirectory()}/$AstraModelsDir/${request.model.filesystemId}"
        val downloaded = findModelFile(fileManager, modelDir)
        if (downloaded != null) {
            return LiteRtLmModelLoadResult.Loaded(
                LiteRtLmModelBundle(
                    id = request.model.filesystemId,
                    displayName = request.model.label,
                    rootPath = modelDir,
                    modelPath = downloaded,
                    sourceModelPath = downloaded,
                    sizeBytes = fileSizeAt(fileManager, downloaded),
                ),
            )
        }

        // Fall back to a model bundled into the app target's resources (added manually via Xcode).
        val bundlePath = NSBundle.mainBundle.pathForResource(request.model.filesystemId, ofType = "litertlm")
            ?: NSBundle.mainBundle.pathForResource("gemma", ofType = "litertlm")
        if (bundlePath != null) {
            return LiteRtLmModelLoadResult.Loaded(
                LiteRtLmModelBundle(
                    id = request.model.filesystemId,
                    displayName = request.model.label,
                    rootPath = bundlePath,
                    modelPath = bundlePath,
                    sourceModelPath = bundlePath,
                    sizeBytes = fileSizeAt(fileManager, bundlePath),
                ),
            )
        }

        return LiteRtLmModelLoadResult.Missing(
            "No LiteRT-LM model found for '${request.model.filesystemId}'. Download it from Model Manager, " +
                "or add a .litertlm file to the iosApp target's bundle resources via Xcode.",
        )
    }

    private fun findModelFile(fileManager: NSFileManager, dir: String): String? {
        val contents = fileManager.contentsOfDirectoryAtPath(dir, error = null) as? List<*> ?: return null
        val match = contents.filterIsInstance<String>()
            .firstOrNull { it.endsWith(".litertlm") || it.endsWith(".task") }
            ?: return null
        return "$dir/$match"
    }
}

/** Sends prompts through the Swift-registered [LiteRtLmBridge.engine]. */
class IosLiteRtLmRuntimeSession : LiteRtLmRuntimeSession {
    override suspend fun generate(
        request: PromptRequest,
        bundle: LiteRtLmModelBundle,
    ): GenerationResult {
        val engine = LiteRtLmBridge.engine
            ?: error(
                "LiteRT-LM native engine is not registered. Check that ContentView.swift calls " +
                    "registerNativeLiteRtLmEngine(...) before MainViewControllerKt.MainViewController().",
            )

        val rawInput = request.userMessage.ifBlank { request.prompt }
        val mark = kotlin.time.TimeSource.Monotonic.markNow()
        val result = suspendCancellableCoroutine { cont ->
            engine.generate(
                prompt = rawInput,
                modelPath = bundle.modelPath,
                maxTokens = request.maxTokens.takeIf { it > 0 } ?: 1024,
            ) { native -> cont.resume(native) }
        }
        val totalLatency = result.latencyMillis.takeIf { it > 0 }
            ?: mark.elapsedNow().inWholeMilliseconds.coerceAtLeast(1L)

        if (result.errorMessage != null || result.text.isNullOrBlank()) {
            error(result.errorMessage ?: "LiteRT-LM generation returned no text.")
        }

        val tokensGenerated = result.tokensGenerated.coerceAtLeast(1)
        val tokensPerSecond = ((tokensGenerated * 1_000L) / totalLatency).toInt().coerceAtLeast(1)

        return GenerationResult(
            text = result.text,
            metrics = GenerationMetrics(
                latencyMillis = totalLatency,
                timeToFirstTokenMillis = totalLatency,
                tokensGenerated = tokensGenerated,
                tokensPerSecond = tokensPerSecond,
                memoryUsageMb = (bundle.sizeBytes / (1024 * 1024)).toInt().coerceAtLeast(1),
            ),
            model = request.model,
            backend = InferenceBackend.LiteRtLm,
            generatedAt = currentIosEdgeTimestamp(),
            runtimeInfo = GenerationRuntimeInfo(
                mode = RuntimeMode.LiteRtLmGenerative,
                inferenceLatencyMillis = totalLatency,
                totalExecutionTimeMillis = totalLatency,
            ),
        )
    }

    override fun close() = Unit
}

private fun documentsDirectory(): String =
    (NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).firstOrNull() as? String)
        ?: platform.Foundation.NSTemporaryDirectory()

private fun fileSizeAt(fileManager: NSFileManager, path: String): Long {
    val attrs = fileManager.attributesOfItemAtPath(path, error = null) ?: return 0L
    return when (val raw = attrs[NSFileSize]) {
        is Long -> raw
        is Int -> raw.toLong()
        is NSNumber -> raw.longLongValue
        else -> 0L
    }
}

@OptIn(ExperimentalTime::class)
private fun currentIosEdgeTimestamp(): String = Clock.System.now().toString()
