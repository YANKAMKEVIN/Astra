package com.kevin.astra.data.ai

import com.kevin.astra.core.ai.BackendStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultBackendCatalogTest {
    @Test
    fun exposesOnlyTheShippedBackends() {
        val catalog = DefaultBackendCatalog()

        val backends = catalog.availableBackends()

        assertEquals(listOf("Mock Engine", "LiteRT-LM"), backends.map { it.displayName })
        assertEquals(BackendStatus.Installed, catalog.backendById("litert-lm")?.status)
        assertEquals(listOf("mock-engine", "litert-lm"), catalog.installedBackends().map { it.id })
        // Removed backends are no longer part of the catalog at all.
        assertNull(catalog.backendById("onnx-runtime"))
        assertNull(catalog.backendById("core-ml"))
        assertNull(catalog.backendById("litert"))
    }

    @Test
    fun prefersLiteRtLmAsTheDefaultWhenInstalled() {
        val catalog = DefaultBackendCatalog()

        assertEquals("litert-lm", catalog.preferredDefaultBackend().id)
        assertEquals("litert-lm", catalog.currentBackend().id)
    }

    @Test
    fun fallsBackToMockWhenLiteRtLmIsNotInstalled() {
        val catalog = DefaultBackendCatalog(
            statusOverrides = mapOf("litert-lm" to { BackendStatus.Unsupported }),
        )

        assertEquals("mock-engine", catalog.preferredDefaultBackend().id)
        assertEquals(listOf("mock-engine"), catalog.installedBackends().map { it.id })
    }

    @Test
    fun selectsInstalledBackendOnly() {
        val catalog = DefaultBackendCatalog()

        assertFalse(catalog.selectBackend("onnx-runtime"))
        assertTrue(catalog.selectBackend("mock-engine"))
        assertEquals("mock-engine", catalog.currentBackend().id)
        assertTrue(catalog.selectBackend("litert-lm"))
        assertEquals("litert-lm", catalog.currentBackend().id)
    }
}
