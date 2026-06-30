package com.kevin.astra.data.settings

import com.kevin.astra.core.ai.PromptIndustry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistentAiConfigurationRepositoryTest {
    @Test
    fun startsWithExpectedDefaults() = runBlocking {
        val repository = testAiConfigurationRepository()

        val configuration = repository.getConfiguration()

        assertEquals("mock-model", configuration.selectedModelId)
        assertEquals("mock-engine", configuration.selectedBackendId)
        assertEquals(PromptIndustry.IndustrialMaintenance, configuration.selectedIndustry)
        assertEquals(0.3, configuration.temperature)
        assertEquals(512, configuration.maxTokens)
        assertEquals(4_096, configuration.contextWindow)
        assertEquals("4-bit", configuration.quantization)
        assertFalse(configuration.experimentalFeaturesEnabled)
    }

    @Test
    fun updatesAndObservesConfiguration() = runBlocking {
        val repository = testAiConfigurationRepository()

        repository.updateSelectedModel("gemma-3-1b")
        repository.updateSelectedBackend("onnx-runtime")
        repository.updateIndustry(PromptIndustry.Energy)
        repository.updateTemperature(0.7)
        repository.updateMaxTokens(1_024)
        repository.updateContextWindow(8_192)
        repository.updateQuantization("8-bit")
        repository.updateExperimentalFeaturesEnabled(true)

        val configuration = repository.observeConfiguration().first()
        assertEquals("gemma-3-1b", configuration.selectedModelId)
        assertEquals("onnx-runtime", configuration.selectedBackendId)
        assertEquals(PromptIndustry.Energy, configuration.selectedIndustry)
        assertEquals(0.7, configuration.temperature)
        assertEquals(1_024, configuration.maxTokens)
        assertEquals(8_192, configuration.contextWindow)
        assertEquals("8-bit", configuration.quantization)
        assertTrue(configuration.experimentalFeaturesEnabled)
    }

    @Test
    fun persistsConfigurationAcrossRepositoryRecreation() = runBlocking {
        val keyValueStore = TestAiConfigurationKeyValueStore()
        val firstRepository = testAiConfigurationRepository(keyValueStore)

        firstRepository.updateSelectedModel("qwen-2-5-1-5b")
        firstRepository.updateSelectedBackend("mock-engine")
        firstRepository.updateTemperature(0.9)

        val recreatedRepository = testAiConfigurationRepository(keyValueStore)
        val configuration = recreatedRepository.getConfiguration()

        assertEquals("qwen-2-5-1-5b", configuration.selectedModelId)
        assertEquals("mock-engine", configuration.selectedBackendId)
        assertEquals(0.9, configuration.temperature)
    }

    @Test
    fun persistsDemoModeEnabledFlag() = runBlocking {
        val keyValueStore = TestAiConfigurationKeyValueStore()
        val repository = testAiConfigurationRepository(keyValueStore)

        assertFalse(repository.getConfiguration().demoModeEnabled)

        repository.updateDemoModeEnabled(true)
        assertTrue(repository.getConfiguration().demoModeEnabled)

        val recreated = testAiConfigurationRepository(keyValueStore)
        assertTrue(recreated.getConfiguration().demoModeEnabled)
    }

    @Test
    fun clampsInferenceParametersBeforePersisting() = runBlocking {
        val repository = testAiConfigurationRepository()

        repository.updateTemperature(2.0)
        repository.updateMaxTokens(8_192)
        repository.updateContextWindow(64)

        val configuration = repository.getConfiguration()
        assertEquals(1.0, configuration.temperature)
        assertEquals(2_048, configuration.maxTokens)
        assertEquals(1_024, configuration.contextWindow)
    }
}
