package com.kevin.astra.data.ai

import com.kevin.astra.core.ai.BackendStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DefaultBackendCatalogTest {
    @Test
    fun exposesCentralBackendListWithMockInstalled() {
        val catalog = DefaultBackendCatalog()

        val backends = catalog.availableBackends()

        assertEquals(
            listOf("Mock Engine", "LiteRT", "LiteRT-LM", "ONNX Runtime", "Core ML", "llama.cpp"),
            backends.map { it.displayName },
        )
        assertEquals(BackendStatus.Installed, catalog.backendById("litert-lm")?.status)
        assertEquals(listOf("mock-engine", "litert-lm"), catalog.installedBackends().map { it.id })
        assertEquals("mock-engine", catalog.currentBackend().id)
        assertTrue(catalog.backendById("onnx-runtime")?.status != BackendStatus.Installed)
    }

    @Test
    fun selectsInstalledBackendOnly() {
        val catalog = DefaultBackendCatalog()

        assertFalse(catalog.selectBackend("onnx-runtime"))
        assertEquals("mock-engine", catalog.currentBackend().id)
        assertTrue(catalog.selectBackend("litert-lm"))
        assertEquals("litert-lm", catalog.currentBackend().id)
        assertTrue(catalog.selectBackend("mock-engine"))
        assertNotNull(catalog.backendById("core-ml"))
    }
}
