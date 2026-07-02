package com.kevin.astra.domain.settings

import com.kevin.astra.core.ai.PromptIndustry
import kotlinx.coroutines.flow.Flow

interface AiConfigurationRepository {
    fun observeConfiguration(): Flow<AiConfiguration>

    suspend fun getConfiguration(): AiConfiguration

    suspend fun updateConfiguration(configuration: AiConfiguration)
    suspend fun updateSelectedModel(modelId: String)
    suspend fun updateSelectedBackend(backendId: String)
    suspend fun updateIndustry(industry: PromptIndustry?)
    suspend fun updateTemperature(temperature: Double)
    suspend fun updateMaxTokens(maxTokens: Int)
    suspend fun updateContextWindow(contextWindow: Int)
    suspend fun updateQuantization(quantization: String)
    suspend fun updateExperimentalFeaturesEnabled(enabled: Boolean)
    suspend fun updateDemoModeEnabled(enabled: Boolean)
    suspend fun updateLightThemeEnabled(enabled: Boolean)
}
