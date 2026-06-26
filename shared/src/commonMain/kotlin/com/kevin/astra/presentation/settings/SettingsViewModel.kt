package com.kevin.astra.presentation.settings

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.domain.settings.AiConfiguration
import com.kevin.astra.domain.settings.AiConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val aiConfigurationRepository: AiConfigurationRepository,
    private val modelCatalog: ModelCatalog,
    private val backendCatalog: BackendCatalog,
    observationScope: CoroutineScope? = null,
) : AstraViewModel<SettingsState, SettingsIntent, SettingsEffect>(
    initialState = SettingsState(
        availableModels = modelCatalog.availableModels(),
        selectedModel = modelCatalog.currentModel(),
        availableBackends = backendCatalog.availableBackends(),
        selectedBackend = backendCatalog.currentBackend(),
    ),
) {
    private val settingsScope = observationScope ?: viewModelScope

    init {
        settingsScope.launch {
            aiConfigurationRepository.observeConfiguration().collect { configuration ->
                updateState { configuration.toSettingsState(modelCatalog, backendCatalog) }
            }
        }
    }

    override fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SelectModel -> {
                if (modelCatalog.selectModel(intent.modelId)) {
                    settingsScope.launch {
                        aiConfigurationRepository.updateSelectedModel(modelCatalog.currentModel().id)
                    }
                }
            }

            is SettingsIntent.SelectBackend -> {
                if (backendCatalog.selectBackend(intent.backendId)) {
                    settingsScope.launch {
                        aiConfigurationRepository.updateSelectedBackend(backendCatalog.currentBackend().id)
                    }
                }
            }

            is SettingsIntent.SelectIndustry ->
                settingsScope.launch { aiConfigurationRepository.updateIndustry(intent.industry) }

            is SettingsIntent.UpdateTemperature ->
                settingsScope.launch { aiConfigurationRepository.updateTemperature(intent.temperature) }

            is SettingsIntent.UpdateMaxTokens ->
                settingsScope.launch { aiConfigurationRepository.updateMaxTokens(intent.maxTokens) }

            is SettingsIntent.UpdateContextWindow ->
                settingsScope.launch { aiConfigurationRepository.updateContextWindow(intent.contextWindow) }

            is SettingsIntent.UpdateQuantization ->
                settingsScope.launch { aiConfigurationRepository.updateQuantization(intent.quantization) }

            is SettingsIntent.ToggleExperimentalFeatures ->
                settingsScope.launch { aiConfigurationRepository.updateExperimentalFeaturesEnabled(intent.enabled) }
        }
    }
}

private fun AiConfiguration.toSettingsState(
    modelCatalog: ModelCatalog,
    backendCatalog: BackendCatalog,
): SettingsState =
    SettingsState(
        availableModels = modelCatalog.availableModels(),
        selectedModel = modelCatalog.modelById(selectedModelId) ?: modelCatalog.currentModel(),
        availableBackends = backendCatalog.availableBackends(),
        selectedBackend = backendCatalog.backendById(selectedBackendId) ?: backendCatalog.currentBackend(),
        selectedIndustry = selectedIndustry,
        temperature = temperature,
        maxTokens = maxTokens,
        contextWindow = contextWindow,
        quantization = quantization,
        experimentalFeaturesEnabled = experimentalFeaturesEnabled,
    )
