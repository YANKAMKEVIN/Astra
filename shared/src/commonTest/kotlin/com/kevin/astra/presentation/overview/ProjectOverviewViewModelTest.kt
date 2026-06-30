package com.kevin.astra.presentation.overview

import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.device.DeviceCapabilities
import com.kevin.astra.core.device.DeviceCapabilityProvider
import com.kevin.astra.core.device.SupportedFeature
import com.kevin.astra.data.ai.DefaultBackendCatalog
import com.kevin.astra.data.ai.DefaultModelCatalog
import com.kevin.astra.data.settings.testAiConfigurationRepository
import com.kevin.astra.domain.modelmanager.StaticModelReadinessProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectOverviewViewModelTest {
    @Test
    fun exposesReadOnlyTechnicalOverview() = runBlocking {
        val observationScope = CoroutineScope(coroutineContext + Job())
        val viewModel = ProjectOverviewViewModel(
            deviceCapabilityProvider = FakeOverviewDeviceCapabilityProvider(),
            aiConfigurationRepository = testAiConfigurationRepository(),
            modelCatalog = DefaultModelCatalog(),
            backendCatalog = DefaultBackendCatalog(),
            modelReadinessProvider = StaticModelReadinessProvider(platformName = "Test"),
            observationScope = observationScope,
        )

        try {
            yield()

            val state = viewModel.state.value
            assertFalse(state.isLoadingCapabilities)
            assertEquals("TestOS", state.capabilities?.platform)
            assertEquals("Mock Engine", state.selectedBackend?.displayName)
            assertEquals("Mock Model", state.selectedModel?.displayName)
            assertEquals("Mock Engine", state.currentRuntime)
            assertEquals(1, state.installedModels.size)
            assertEquals(10, state.availableModels.size)
            assertTrue(state.architectureItems.any { it.title == "RoutingInferenceEngine" })
            assertTrue("Model Manager" in state.aiFeatures)
            assertTrue(state.documentationLinks.any { it.path == "docs/03_Platform_Architecture.md" })
        } finally {
            observationScope.cancel()
        }
    }
}

private class FakeOverviewDeviceCapabilityProvider : DeviceCapabilityProvider {
    override suspend fun getCapabilities(): DeviceCapabilities =
        DeviceCapabilities(
            platform = "TestOS",
            osVersion = "1.0",
            deviceModel = "Test Device",
            cpuName = "Test CPU",
            gpuName = "Test GPU",
            npuAvailable = false,
            npuName = "Not detected",
            totalMemoryMb = 8_192,
            availableMemoryMb = 4_096,
            storageAvailableGb = 128.0,
            supportedBackends = listOf(InferenceBackend.Mock),
            supportedFeatures = listOf(
                SupportedFeature.LocalAI,
                SupportedFeature.DocumentQA,
                SupportedFeature.Benchmark,
                SupportedFeature.OfflineMode,
            ),
        )
}
