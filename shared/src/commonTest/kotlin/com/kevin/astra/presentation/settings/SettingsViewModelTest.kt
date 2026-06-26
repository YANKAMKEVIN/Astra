package com.kevin.astra.presentation.settings

import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.data.ai.DefaultBackendCatalog
import com.kevin.astra.data.ai.DefaultModelCatalog
import com.kevin.astra.data.settings.InMemoryAiConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest {
    @Test
    fun exposesRepositoryDefaults() {
        val viewModel = testViewModel()

        val state = viewModel.state.value

        assertEquals("mock-model", state.selectedModel?.id)
        assertEquals(
            listOf("Mock Model", "Gemma 3 1B", "Phi-3 Mini", "Llama 3.2 3B", "Qwen 2.5 1.5B"),
            state.availableModels.map { it.displayName },
        )
        assertEquals("mock-engine", state.selectedBackend?.id)
        assertEquals(
            listOf("Mock Engine", "LiteRT", "ONNX Runtime", "Core ML", "llama.cpp"),
            state.availableBackends.map { it.displayName },
        )
        assertEquals(PromptIndustry.IndustrialMaintenance, state.selectedIndustry)
        assertEquals(0.3, state.temperature)
        assertEquals(512, state.maxTokens)
        assertEquals(4_096, state.contextWindow)
        assertEquals("4-bit", state.quantization)
        assertFalse(state.experimentalFeaturesEnabled)
    }

    @Test
    fun keepsUnavailableModelsAndBackendsUnselected() {
        val viewModel = testViewModel()

        viewModel.dispatch(SettingsIntent.SelectModel("gemma-3-1b"))
        viewModel.dispatch(SettingsIntent.SelectBackend("onnx-runtime"))

        val state = viewModel.state.value
        assertEquals("mock-model", state.selectedModel?.id)
        assertEquals("mock-engine", state.selectedBackend?.id)
    }

    @Test
    fun updatesParametersAndExperimentalToggle() {
        val viewModel = testViewModel()

        viewModel.dispatch(SettingsIntent.SelectIndustry(PromptIndustry.Healthcare))
        viewModel.dispatch(SettingsIntent.UpdateTemperature(0.6))
        viewModel.dispatch(SettingsIntent.UpdateMaxTokens(1_024))
        viewModel.dispatch(SettingsIntent.UpdateContextWindow(8_192))
        viewModel.dispatch(SettingsIntent.UpdateQuantization("8-bit"))
        viewModel.dispatch(SettingsIntent.ToggleExperimentalFeatures(true))

        val state = viewModel.state.value
        assertEquals(PromptIndustry.Healthcare, state.selectedIndustry)
        assertEquals(0.6, state.temperature)
        assertEquals(1_024, state.maxTokens)
        assertEquals(8_192, state.contextWindow)
        assertEquals("8-bit", state.quantization)
        assertTrue(state.experimentalFeaturesEnabled)
    }

    private fun testViewModel(): SettingsViewModel =
        SettingsViewModel(
            aiConfigurationRepository = InMemoryAiConfigurationRepository(),
            modelCatalog = DefaultModelCatalog(),
            backendCatalog = DefaultBackendCatalog(),
            observationScope = CoroutineScope(EmptyCoroutineContext),
        )
}
