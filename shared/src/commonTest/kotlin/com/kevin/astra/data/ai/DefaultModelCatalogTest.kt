package com.kevin.astra.data.ai

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

        assertEquals(
            listOf("Mock Model", "Gemma 3 1B", "Phi-3 Mini", "Llama 3.2 3B", "Qwen 2.5 1.5B"),
            models.map { it.displayName },
        )
        assertEquals(listOf("mock-model"), catalog.installedModels().map { it.id })
        assertEquals(ModelProvider.Google, catalog.modelById("gemma-3-1b")?.provider)
        assertTrue(models.drop(1).all { it.status == ModelStatus.Available })
    }

    @Test
    fun keepsCurrentModelOnInstalledCatalogEntryOnly() {
        val catalog = DefaultModelCatalog()

        assertEquals("mock-model", catalog.currentModel().id)
        assertFalse(catalog.selectModel("gemma-3-1b"))
        assertEquals("mock-model", catalog.currentModel().id)
        assertTrue(catalog.selectModel("mock-model"))
        assertEquals("mock-model", catalog.currentModel().id)
        assertNotNull(catalog.modelById("qwen-2-5-1-5b"))
    }
}
