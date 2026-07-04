package com.kevin.astra.data.ai

import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.ModelProvider
import com.kevin.astra.core.ai.ModelStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DefaultModelCatalogTest {
    @Test
    fun exposesCentralModelListWithMockInstalled() {
        val catalog = DefaultModelCatalog()

        val models = catalog.availableModels()

        assertEquals(12, models.size)
        assertTrue(models.map { it.displayName }.containsAll(
            listOf("Mock Model", "Gemma 3 1B", "Gemma 4 E2B", "Phi-3 Mini", "Llama 3.2 3B", "Qwen 2.5 1.5B")
        ))
        assertEquals(listOf("mock-model"), catalog.installedModels().map { it.id })
        assertEquals(ModelProvider.Google, catalog.modelById("gemma-3-1b")?.provider)
        assertTrue(models.drop(1).none { it.status == ModelStatus.Installed })
    }

    @Test
    fun selectsAvailableModelsAsRuntimeTargets() {
        val catalog = DefaultModelCatalog()

        assertEquals("mock-model", catalog.currentModel().id)
        assertTrue(catalog.selectModel("gemma-3-1b"))
        assertEquals("gemma-3-1b", catalog.currentModel().id)
        assertTrue(catalog.selectModel("mock-model"))
        assertEquals("mock-model", catalog.currentModel().id)
        assertNotNull(catalog.modelById("qwen-2-5-1-5b"))
    }

    @Test
    fun hidesModelsWithoutABackendThePlatformProvides() {
        // A platform that only ships Mock + LiteRT-LM (both iOS and Android today).
        val catalog = DefaultModelCatalog(
            usableBackends = setOf(InferenceBackend.Mock, InferenceBackend.LiteRtLm),
        )

        val ids = catalog.availableModels().map { it.id }

        assertEquals(9, ids.size)
        // ONNX / llama.cpp-only models have no runtime here and are not listed at all.
        assertFalse(ids.contains("phi-3-mini"))
        assertFalse(ids.contains("qwen-2-5-1-5b"))
        assertFalse(ids.contains("llama-3-2-3b"))
        // Mock and every LiteRT-LM model remain — including the Gemma 4 builds and llama-3-2-1b.
        assertTrue(ids.containsAll(listOf("mock-model", "gemma-3-1b", "gemma-4-e2b", "gemma-4-e4b", "llama-3-2-1b")))
    }
}
