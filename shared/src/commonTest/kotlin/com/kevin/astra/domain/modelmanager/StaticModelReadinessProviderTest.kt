package com.kevin.astra.domain.modelmanager

import com.kevin.astra.data.ai.DefaultModelCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StaticModelReadinessProviderTest {
    @Test
    fun exposesInstalledMockFallbackAndUnsupportedLiteRtLmOnNonAndroidPlatforms() {
        val readiness = StaticModelReadinessProvider(platformName = "iOS")
            .readinessFor(DefaultModelCatalog().availableModels())

        val mock = readiness.first { it.modelId == "mock-model" }
        val gemma = readiness.first { it.modelId == "gemma-3-1b" }

        assertEquals(ModelReadinessStatus.Installed, mock.status)
        assertTrue(mock.readinessMessage.contains("Mock fallback"))
        assertEquals("Built-in mock runtime", mock.localPath)

        assertEquals(ModelReadinessStatus.UnsupportedPlatform, gemma.status)
        assertTrue(gemma.readinessMessage.contains("Use Mock fallback"))
        assertEquals("~0.8–2 GB quantized", gemma.expectedSize)
    }
}
