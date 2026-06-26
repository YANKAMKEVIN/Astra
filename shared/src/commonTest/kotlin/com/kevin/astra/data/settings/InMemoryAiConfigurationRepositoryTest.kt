package com.kevin.astra.data.settings

import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.PromptIndustry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InMemoryAiConfigurationRepositoryTest {
    @Test
    fun startsWithExpectedDefaults() {
        val repository = InMemoryAiConfigurationRepository()

        val configuration = repository.currentConfiguration.value

        assertEquals(AiModel.Mock, configuration.selectedModel)
        assertEquals(InferenceBackend.Mock, configuration.selectedBackend)
        assertEquals(PromptIndustry.IndustrialMaintenance, configuration.selectedIndustry)
        assertEquals(0.3, configuration.temperature)
        assertEquals(512, configuration.maxTokens)
        assertEquals(4_096, configuration.contextWindow)
        assertEquals("4-bit", configuration.quantization)
        assertFalse(configuration.experimentalFeaturesEnabled)
    }

    @Test
    fun ignoresUnavailableModelAndBackend() {
        val repository = InMemoryAiConfigurationRepository()

        repository.updateModel(AiModel.Gemma)
        repository.updateBackend(InferenceBackend.LiteRt)

        assertEquals(AiModel.Mock, repository.currentConfiguration.value.selectedModel)
        assertEquals(InferenceBackend.Mock, repository.currentConfiguration.value.selectedBackend)
    }

    @Test
    fun updatesInferenceParametersInMemory() {
        val repository = InMemoryAiConfigurationRepository()

        repository.updateIndustry(PromptIndustry.Energy)
        repository.updateTemperature(0.7)
        repository.updateMaxTokens(1_024)
        repository.updateContextWindow(8_192)
        repository.updateQuantization("8-bit")
        repository.updateExperimentalFeaturesEnabled(true)

        val configuration = repository.currentConfiguration.value
        assertEquals(PromptIndustry.Energy, configuration.selectedIndustry)
        assertEquals(0.7, configuration.temperature)
        assertEquals(1_024, configuration.maxTokens)
        assertEquals(8_192, configuration.contextWindow)
        assertEquals("8-bit", configuration.quantization)
        assertTrue(configuration.experimentalFeaturesEnabled)
    }
}
