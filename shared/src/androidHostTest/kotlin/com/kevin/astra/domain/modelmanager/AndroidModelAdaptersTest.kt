package com.kevin.astra.domain.modelmanager

import android.content.ContextWrapper
import android.content.res.AssetManager
import com.kevin.astra.core.ai.AndroidAssetLiteRtLmModelLoader
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.LiteRtLmModelLoadResult
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.PromptRequest
import com.kevin.astra.data.ai.DefaultModelCatalog
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The Android adapters that find, download and load model files, run against a temporary
 * filesDir and the same kind of local HTTP server as [ResumableDownloaderTest]. No Robolectric:
 * the Context only answers filesDir / cacheDir, and has no assets (APK-bundled models are not
 * covered here).
 */
class AndroidModelAdaptersTest {
    private val root: File = Files.createTempDirectory("astra-adapters").toFile()
    private val context = HostContext(filesDir = File(root, "files"), cacheDir = File(root, "cache"))
    private val payload = Random(7).nextBytes(300_000)
    private var server: HttpServer? = null

    @AfterTest
    fun tearDown() {
        server?.stop(0)
        root.deleteRecursively()
    }

    /** filesDir / cacheDir in a temp folder; anything else (assets included) is unavailable. */
    private class HostContext(private val filesDir: File, private val cacheDir: File) : ContextWrapper(null) {
        override fun getFilesDir(): File = filesDir.also { it.mkdirs() }
        override fun getCacheDir(): File = cacheDir.also { it.mkdirs() }
        override fun getAssets(): AssetManager = throw UnsupportedOperationException("no assets on the host")
    }

    private fun serve(status: Int = 200): String {
        val http = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        http.createContext("/model.litertlm") { exchange ->
            if (status != 200) {
                exchange.sendResponseHeaders(status, -1)
                exchange.close()
                return@createContext
            }
            exchange.sendResponseHeaders(200, payload.size.toLong())
            exchange.responseBody.use { it.write(payload) }
        }
        http.start()
        server = http
        return "http://127.0.0.1:${http.address.port}/model.litertlm"
    }

    /** A downloadable LiteRT-LM model of the real catalog. */
    private val model: LocalModel = DefaultModelCatalog().availableModels()
        .first { InferenceBackend.LiteRtLm in it.supportedBackends && it.downloadUrl != null }

    private fun request(url: String) = ModelDownloadRequest(
        modelId = model.id,
        displayName = model.displayName,
        url = url,
        fileName = "model.litertlm",
    )

    private fun modelDir() = File(context.filesDir, "astra-models/${model.id}")

    @Test
    fun theDownloadFolderOfEveryLiteRtLmModelIsTheOneTheDetectionLooksIn() {
        // Downloads go to astra-models/<catalog id>; readiness and the engine look in
        // astra-models/<runtime filesystem id>. They must be the same folder for every model.
        DefaultModelCatalog().availableModels()
            .filter { InferenceBackend.LiteRtLm in it.supportedBackends }
            .forEach { assertEquals(it.id, it.runtimeModel.filesystemId, "folder mismatch for ${it.id}") }
    }

    @Test
    fun aDownloadedModelIsDetectedByTheManagerTheReadinessAndTheEngineLoader() = runBlocking {
        val manager = AndroidModelDownloadManager(context)

        manager.download(request(serve()))

        val state = manager.downloadState.value
        assertIs<ModelDownloadState.Completed>(state)
        val file = File(modelDir(), "model.litertlm")
        assertEquals(file.absolutePath, state.localPath)
        assertContentEquals(payload, file.readBytes())
        assertFalse(File(modelDir(), "model.litertlm.tmp").exists())

        assertEquals(mapOf(model.id to file.absolutePath), manager.getInstalledModelPaths())
        assertTrue(manager.getStorageUsageMb() > 0.28f)

        val readiness = AndroidModelReadinessProvider(context).readinessFor(listOf(model)).single()
        assertEquals(ModelReadinessStatus.Installed, readiness.status)
        assertTrue(readiness.isDownloadedToFilesDir)
        assertEquals(file.absolutePath, readiness.requiredFiles.single().path)

        val loaded = AndroidAssetLiteRtLmModelLoader(context).loadModel(PromptRequest(prompt = "hi", model = model.runtimeModel))
        assertIs<LiteRtLmModelLoadResult.Loaded>(loaded)
        assertEquals(file.absolutePath, loaded.bundle.modelPath)
        assertEquals(payload.size.toLong(), loaded.bundle.sizeBytes)
    }

    @Test
    fun anUnauthorizedDownloadFailsWithTheHuggingFaceHintAndLeavesNothingInstalled() = runBlocking {
        val manager = AndroidModelDownloadManager(context)

        manager.download(request(serve(status = 401)))

        val state = manager.downloadState.value
        assertIs<ModelDownloadState.Failed>(state)
        assertTrue(state.reason.contains("HuggingFace"), state.reason)
        assertTrue(manager.getInstalledModelPaths().isEmpty())
        assertEquals(
            ModelReadinessStatus.ModelRequired,
            AndroidModelReadinessProvider(context).readinessFor(listOf(model)).single().status,
        )
    }

    @Test
    fun aPartialDownloadOrAStrayFileIsNotAnInstalledModel() {
        modelDir().mkdirs()
        File(modelDir(), "model.litertlm.tmp").writeBytes(payload.copyOf(1_000))
        File(modelDir(), "notes.txt").writeText("not a model")
        File(context.filesDir, "astra-models/stray-file.litertlm").writeText("not in a model folder")

        assertTrue(AndroidModelDownloadManager(context).getInstalledModelPaths().isEmpty())
        val readiness = AndroidModelReadinessProvider(context).readinessFor(listOf(model)).single()
        assertEquals(ModelReadinessStatus.ModelRequired, readiness.status)
        assertFalse(readiness.isDownloadedToFilesDir)
        assertIs<LiteRtLmModelLoadResult.Missing>(
            runBlocking { AndroidAssetLiteRtLmModelLoader(context).loadModel(PromptRequest(prompt = "hi", model = model.runtimeModel)) },
        )
    }

    @Test
    fun theEngineLoaderPrefersTheLiteRtLmBundleOverASplitModel() = runBlocking {
        modelDir().mkdirs()
        File(modelDir(), "weights.tflite").writeBytes(ByteArray(10))
        val bundle = File(modelDir(), "model.litertlm").apply { writeBytes(ByteArray(20)) }

        val loaded = AndroidAssetLiteRtLmModelLoader(context).loadModel(PromptRequest(prompt = "hi", model = model.runtimeModel))

        assertIs<LiteRtLmModelLoadResult.Loaded>(loaded)
        assertEquals(bundle.absolutePath, loaded.bundle.modelPath)
    }

    @Test
    fun deletingAModelRemovesItEverywhere() = runBlocking {
        val manager = AndroidModelDownloadManager(context)
        manager.download(request(serve()))

        assertTrue(manager.deleteModel(model.id))

        assertTrue(manager.getInstalledModelPaths().isEmpty())
        assertEquals(0f, manager.getStorageUsageMb())
        assertFalse(manager.deleteModel(model.id))
        assertEquals(
            ModelReadinessStatus.ModelRequired,
            AndroidModelReadinessProvider(context).readinessFor(listOf(model)).single().status,
        )
    }

    @Test
    fun aModelWithoutDownloadUrlAsksForBundledFilesAndTheMockNeedsNone() {
        val manual = model.copy(downloadUrl = null)
        val mock = DefaultModelCatalog().availableModels().first { it.id == "mock-model" }

        val (manualReadiness, mockReadiness) = AndroidModelReadinessProvider(context).readinessFor(listOf(manual, mock))

        assertEquals(ModelReadinessStatus.MissingFiles, manualReadiness.status)
        assertEquals(ModelReadinessStatus.Installed, mockReadiness.status)
        assertFalse(mockReadiness.isDownloadedToFilesDir)
    }

    @Test
    fun withoutAContextNothingIsInstalledAndNothingCrashes() {
        val readiness = AndroidModelReadinessProvider(context = null).readinessFor(listOf(model)).single()
        assertEquals(ModelReadinessStatus.ModelRequired, readiness.status)
    }
}
