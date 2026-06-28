package com.kevin.astra.presentation.demo

import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.device.DeviceCapabilities
import com.kevin.astra.core.device.DeviceCapabilityProvider
import com.kevin.astra.core.device.SupportedFeature
import com.kevin.astra.data.ai.DefaultBackendCatalog
import com.kevin.astra.data.ai.DefaultModelCatalog
import com.kevin.astra.data.demo.StaticDemoScenarioCatalog
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

class DemoViewModelTest {
    @Test
    fun exposesDemoReadinessOverview() = runBlocking {
        val observationScope = CoroutineScope(coroutineContext + Job())
        val viewModel = testViewModel(observationScope)
        try {
            yield()

            val state = viewModel.state.value
            assertFalse(state.isLoadingCapabilities)
            assertEquals("TestOS", state.capabilities?.platform)
            assertEquals("Mock Model", state.selectedModel?.displayName)
            assertEquals("Mock Engine", state.selectedBackend?.displayName)
            assertEquals(
                listOf("Device Ready", "Runtime Ready", "Model Ready", "Benchmark Ready", "Documents Ready"),
                state.readinessIndicators.map { it.label },
            )
            assertTrue(state.isDemoReady)
        } finally {
            observationScope.cancel()
        }
    }

    @Test
    fun guidedDemoCanMoveForwardAndSelectSteps() = runBlocking {
        val observationScope = CoroutineScope(coroutineContext + Job())
        val viewModel = testViewModel(observationScope)
        try {
            yield()

            viewModel.dispatch(DemoIntent.NextStep)
            assertEquals(DemoStep.RuntimeSelection, viewModel.state.value.currentStep)
            assertTrue(DemoStep.DeviceCapabilities in viewModel.state.value.completedSteps)

            viewModel.dispatch(DemoIntent.SelectStep(DemoStep.ModelManager))
            assertEquals(DemoStep.ModelManager, viewModel.state.value.currentStep)
            assertTrue(DemoStep.TaskEvaluation in viewModel.state.value.completedSteps)

            viewModel.dispatch(DemoIntent.PreviousStep)
            assertEquals(DemoStep.TaskEvaluation, viewModel.state.value.currentStep)
        } finally {
            observationScope.cancel()
        }
    }

    private fun testViewModel(observationScope: CoroutineScope): DemoViewModel =
        DemoViewModel(
            deviceCapabilityProvider = FakeDemoDeviceCapabilityProvider(),
            aiConfigurationRepository = testAiConfigurationRepository(),
            modelCatalog = DefaultModelCatalog(),
            backendCatalog = DefaultBackendCatalog(),
            modelReadinessProvider = StaticModelReadinessProvider(platformName = "Test"),
            demoScenarioCatalog = StaticDemoScenarioCatalog(),
            observationScope = observationScope,
        )
}

private class FakeDemoDeviceCapabilityProvider : DeviceCapabilityProvider {
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
