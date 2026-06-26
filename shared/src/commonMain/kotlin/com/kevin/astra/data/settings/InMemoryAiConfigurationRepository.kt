package com.kevin.astra.data.settings

import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.domain.settings.AiConfiguration
import com.kevin.astra.domain.settings.AiConfigurationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryAiConfigurationRepository : AiConfigurationRepository {
    private val mutableConfiguration = MutableStateFlow(AiConfiguration())

    override val currentConfiguration: StateFlow<AiConfiguration> =
        mutableConfiguration.asStateFlow()

    override fun updateModel(model: AiModel) {
        if (model == AiModel.Mock) {
            mutableConfiguration.update { it.copy(selectedModel = model) }
        }
    }

    override fun updateBackend(backend: InferenceBackend) {
        if (backend == InferenceBackend.Mock) {
            mutableConfiguration.update { it.copy(selectedBackend = backend) }
        }
    }

    override fun updateIndustry(industry: PromptIndustry) {
        mutableConfiguration.update { it.copy(selectedIndustry = industry) }
    }

    override fun updateTemperature(temperature: Double) {
        mutableConfiguration.update {
            it.copy(temperature = temperature.coerceIn(0.0, 1.0))
        }
    }

    override fun updateMaxTokens(maxTokens: Int) {
        mutableConfiguration.update {
            it.copy(maxTokens = maxTokens.coerceIn(128, 2_048))
        }
    }

    override fun updateContextWindow(contextWindow: Int) {
        mutableConfiguration.update {
            it.copy(contextWindow = contextWindow.coerceIn(1_024, 16_384))
        }
    }

    override fun updateQuantization(quantization: String) {
        mutableConfiguration.update { it.copy(quantization = quantization) }
    }

    override fun updateExperimentalFeaturesEnabled(enabled: Boolean) {
        mutableConfiguration.update {
            it.copy(experimentalFeaturesEnabled = enabled)
        }
    }
}
