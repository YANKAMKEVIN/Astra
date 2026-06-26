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
            listOf("Mock Engine", "LiteRT", "ONNX Runtime", "Core ML", "llama.cpp"),
            backends.map { it.displayName },
        )
        assertEquals(listOf("mock-engine"), catalog.installedBackends().map { it.id })
        assertEquals("mock-engine", catalog.currentBackend().id)
        assertTrue(backends.drop(1).all { it.status != BackendStatus.Installed })
    }

    @Test
    fun selectsInstalledBackendOnly() {
        val catalog = DefaultBackendCatalog()

        assertFalse(catalog.selectBackend("onnx-runtime"))
        assertEquals("mock-engine", catalog.currentBackend().id)
        assertTrue(catalog.selectBackend("mock-engine"))
        assertNotNull(catalog.backendById("core-ml"))
    }
}
