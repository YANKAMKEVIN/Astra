package com.kevin.astra.data.settings

import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.domain.settings.AiConfiguration
import com.kevin.astra.domain.settings.AiConfigurationRepository
import com.kevin.astra.domain.settings.DemoModeHolder
import com.kevin.astra.domain.settings.ThemeHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PersistentAiConfigurationRepository(
    private val localDataSource: AiConfigurationLocalDataSource,
) : AiConfigurationRepository {
    private val mutableConfiguration = MutableStateFlow(localDataSource.loadConfiguration().normalized()).also {
        DemoModeHolder.set(it.value.demoModeEnabled)
        ThemeHolder.set(it.value.lightThemeEnabled)
    }

    override fun observeConfiguration(): Flow<AiConfiguration> =
        mutableConfiguration.asStateFlow()

    override suspend fun getConfiguration(): AiConfiguration =
        mutableConfiguration.value

    override suspend fun updateConfiguration(configuration: AiConfiguration) {
        val normalized = configuration.normalized()
        localDataSource.saveConfiguration(normalized)
        mutableConfiguration.value = normalized
        DemoModeHolder.set(normalized.demoModeEnabled)
        ThemeHolder.set(normalized.lightThemeEnabled)
    }

    override suspend fun updateSelectedModel(modelId: String) {
        updateConfiguration(mutableConfiguration.value.copy(selectedModelId = modelId))
    }

    override suspend fun updateSelectedBackend(backendId: String) {
        updateConfiguration(mutableConfiguration.value.copy(selectedBackendId = backendId))
    }

    override suspend fun updateIndustry(industry: PromptIndustry) {
        updateConfiguration(mutableConfiguration.value.copy(selectedIndustry = industry))
    }

    override suspend fun updateTemperature(temperature: Double) {
        updateConfiguration(mutableConfiguration.value.copy(temperature = temperature))
    }

    override suspend fun updateMaxTokens(maxTokens: Int) {
        updateConfiguration(mutableConfiguration.value.copy(maxTokens = maxTokens))
    }

    override suspend fun updateContextWindow(contextWindow: Int) {
        updateConfiguration(mutableConfiguration.value.copy(contextWindow = contextWindow))
    }

    override suspend fun updateQuantization(quantization: String) {
        updateConfiguration(mutableConfiguration.value.copy(quantization = quantization))
    }

    override suspend fun updateExperimentalFeaturesEnabled(enabled: Boolean) {
        updateConfiguration(mutableConfiguration.value.copy(experimentalFeaturesEnabled = enabled))
    }

    override suspend fun updateDemoModeEnabled(enabled: Boolean) {
        updateConfiguration(mutableConfiguration.value.copy(demoModeEnabled = enabled))
    }

    override suspend fun updateLightThemeEnabled(enabled: Boolean) {
        updateConfiguration(mutableConfiguration.value.copy(lightThemeEnabled = enabled))
    }
}

private fun AiConfiguration.normalized(): AiConfiguration =
    copy(
        temperature = temperature.coerceIn(0.0, 1.0),
        maxTokens = maxTokens.coerceIn(128, 2_048),
        contextWindow = contextWindow.coerceIn(1_024, 16_384),
    )
