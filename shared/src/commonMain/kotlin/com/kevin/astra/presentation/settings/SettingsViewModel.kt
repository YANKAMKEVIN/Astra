package com.kevin.astra.presentation.settings

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.domain.settings.AiConfiguration
import com.kevin.astra.domain.settings.AiConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val aiConfigurationRepository: AiConfigurationRepository,
    private val modelCatalog: ModelCatalog,
    observationScope: CoroutineScope? = null,
) : AstraViewModel<SettingsState, SettingsIntent, SettingsEffect>(
    initialState = aiConfigurationRepository.currentConfiguration.value.toSettingsState(modelCatalog),
) {
    init {
        (observationScope ?: viewModelScope).launch {
            aiConfigurationRepository.currentConfiguration.collect { configuration ->
                updateState { configuration.toSettingsState(modelCatalog) }
            }
        }
    }

    override fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SelectModel -> {
                if (modelCatalog.selectModel(intent.modelId)) {
                    aiConfigurationRepository.updateModel(modelCatalog.currentModel().runtimeModel)
                }
            }

            is SettingsIntent.SelectBackend -> {
                if (intent.backend == InferenceBackend.Mock) {
                    aiConfigurationRepository.updateBackend(intent.backend)
                }
            }

            is SettingsIntent.SelectIndustry ->
                aiConfigurationRepository.updateIndustry(intent.industry)

            is SettingsIntent.UpdateTemperature ->
                aiConfigurationRepository.updateTemperature(intent.temperature)

            is SettingsIntent.UpdateMaxTokens ->
                aiConfigurationRepository.updateMaxTokens(intent.maxTokens)

            is SettingsIntent.UpdateContextWindow ->
                aiConfigurationRepository.updateContextWindow(intent.contextWindow)

            is SettingsIntent.UpdateQuantization ->
                aiConfigurationRepository.updateQuantization(intent.quantization)

            is SettingsIntent.ToggleExperimentalFeatures ->
                aiConfigurationRepository.updateExperimentalFeaturesEnabled(intent.enabled)
        }
        syncStateFromRepository()
    }

    private fun syncStateFromRepository() {
        updateState {
            aiConfigurationRepository.currentConfiguration.value.toSettingsState(modelCatalog)
        }
    }
}

private fun AiConfiguration.toSettingsState(modelCatalog: ModelCatalog): SettingsState =
    SettingsState(
        availableModels = modelCatalog.availableModels(),
        selectedModel = modelCatalog.currentModel(),
        selectedBackend = selectedBackend,
        selectedIndustry = selectedIndustry,
        temperature = temperature,
        maxTokens = maxTokens,
        contextWindow = contextWindow,
        quantization = quantization,
        experimentalFeaturesEnabled = experimentalFeaturesEnabled,
    )
