@file:OptIn(ExperimentalForeignApi::class)

package com.kevin.astra.domain.modelmanager

import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.data.ai.DefaultModelCatalog
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.writeToFile
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Detection of a downloaded model by [IosModelReadinessProvider], on the real file system of the
 * test process (its own Documents directory, never the app's). Mirrors AndroidModelAdaptersTest.
 */
class IosModelReadinessProviderTest {

    private val fileManager = NSFileManager.defaultManager

    /** A downloadable LiteRT-LM model of the real catalog. */
    private val model: LocalModel = DefaultModelCatalog().availableModels()
        .first { InferenceBackend.LiteRtLm in it.supportedBackends && it.downloadUrl != null }

    private val modelDir: String by lazy {
        val documents = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String ?: NSTemporaryDirectory()
        "$documents/astra-models/${model.runtimeModel.filesystemId}"
    }

    @BeforeTest
    fun setUp() {
        fileManager.removeItemAtPath(modelDir, error = null)
        fileManager.createDirectoryAtPath(modelDir, withIntermediateDirectories = true, attributes = null, error = null)
    }

    @AfterTest
    fun tearDown() {
        fileManager.removeItemAtPath(modelDir, error = null)
    }

    private fun createFile(name: String) {
        @Suppress("CAST_NEVER_SUCCEEDS")
        val written = ("fake model" as NSString)
            .writeToFile("$modelDir/$name", atomically = true, encoding = NSUTF8StringEncoding, error = null)
        assertTrue(written, "could not create $modelDir/$name")
    }

    private fun readiness(vararg models: LocalModel) = IosModelReadinessProvider().readinessFor(models.toList())

    @Test
    fun aDownloadedModelIsDetectedAsInstalled() {
        createFile("model.litertlm")

        val readiness = readiness(model).single()

        assertEquals(ModelReadinessStatus.Installed, readiness.status)
        assertTrue(readiness.isDownloadedToFilesDir)
        assertEquals("$modelDir/model.litertlm", readiness.requiredFiles.single().path)
    }

    @Test
    fun aPartialDownloadOrAStrayFileIsNotAnInstalledModel() {
        createFile("model.litertlm.tmp")
        createFile("notes.txt")

        val readiness = readiness(model).single()

        assertEquals(ModelReadinessStatus.ModelRequired, readiness.status)
        assertFalse(readiness.isDownloadedToFilesDir)
    }

    @Test
    fun aModelWithoutDownloadUrlAsksForBundledFilesAndTheMockNeedsNone() {
        val manual = model.copy(downloadUrl = null)
        val mock = DefaultModelCatalog().availableModels().first { it.id == "mock-model" }

        val (manualReadiness, mockReadiness) = readiness(manual, mock)

        assertEquals(ModelReadinessStatus.MissingFiles, manualReadiness.status)
        assertEquals(ModelReadinessStatus.Installed, mockReadiness.status)
        assertFalse(mockReadiness.isDownloadedToFilesDir)
    }
}
