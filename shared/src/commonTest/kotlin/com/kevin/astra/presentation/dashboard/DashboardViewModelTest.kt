package com.kevin.astra.presentation.dashboard

import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.device.DeviceCapabilities
import com.kevin.astra.core.device.DeviceCapabilityProvider
import com.kevin.astra.core.device.SupportedFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashboardViewModelTest {
    @Test
    fun loadsCapabilitiesOnStart() = runBlocking {
        val viewModel = DashboardViewModel(
            deviceCapabilityProvider = FakeDeviceCapabilityProvider(),
            dashboardScope = CoroutineScope(coroutineContext),
        )

        yield()

        val state = viewModel.state.value
        assertFalse(state.isLoadingCapabilities)
        assertEquals("TestOS", state.capabilities?.platform)
        assertEquals("Test Device", state.capabilities?.deviceModel)
        assertTrue(SupportedFeature.OfflineMode in state.capabilities?.supportedFeatures.orEmpty())
    }

    @Test
    fun exposesErrorWhenProviderFails() = runBlocking {
        val viewModel = DashboardViewModel(
            deviceCapabilityProvider = object : DeviceCapabilityProvider {
                override suspend fun getCapabilities(): DeviceCapabilities {
                    error("boom")
                }
            },
            dashboardScope = CoroutineScope(coroutineContext),
        )

        yield()

        val state = viewModel.state.value
        assertFalse(state.isLoadingCapabilities)
        assertEquals("Unable to read device capabilities.", state.error)
    }
}

private class FakeDeviceCapabilityProvider : DeviceCapabilityProvider {
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
