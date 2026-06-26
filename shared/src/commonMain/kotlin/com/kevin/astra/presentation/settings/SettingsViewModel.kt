package com.kevin.astra.presentation.settings

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.domain.settings.AiConfiguration
import com.kevin.astra.domain.settings.AiConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val aiConfigurationRepository: AiConfigurationRepository,
    observationScope: CoroutineScope? = null,
) : AstraViewModel<SettingsState, SettingsIntent, SettingsEffect>(
    initialState = aiConfigurationRepository.currentConfiguration.value.toSettingsState(),
) {
    init {
        (observationScope ?: viewModelScope).launch {
            aiConfigurationRepository.currentConfiguration.collect { configuration ->
                updateState { configuration.toSettingsState() }
            }
        }
    }

    override fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SelectModel -> {
                if (intent.model == AiModel.Mock) {
                    aiConfigurationRepository.updateModel(intent.model)
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
            aiConfigurationRepository.currentConfiguration.value.toSettingsState()
        }
    }
}

private fun AiConfiguration.toSettingsState(): SettingsState =
    SettingsState(
        selectedModel = selectedModel,
        selectedBackend = selectedBackend,
        selectedIndustry = selectedIndustry,
        temperature = temperature,
        maxTokens = maxTokens,
        contextWindow = contextWindow,
        quantization = quantization,
        experimentalFeaturesEnabled = experimentalFeaturesEnabled,
    )
