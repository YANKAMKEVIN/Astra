package com.kevin.astra.domain.settings

import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.PromptIndustry
import kotlinx.coroutines.flow.StateFlow

interface AiConfigurationRepository {
    val currentConfiguration: StateFlow<AiConfiguration>

    fun updateModel(model: AiModel)
    fun updateBackend(backend: InferenceBackend)
    fun updateIndustry(industry: PromptIndustry)
    fun updateTemperature(temperature: Double)
    fun updateMaxTokens(maxTokens: Int)
    fun updateContextWindow(contextWindow: Int)
    fun updateQuantization(quantization: String)
    fun updateExperimentalFeaturesEnabled(enabled: Boolean)
}
