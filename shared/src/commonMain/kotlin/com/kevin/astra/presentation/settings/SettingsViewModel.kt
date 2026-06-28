package com.kevin.astra.presentation.settings

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.domain.modelmanager.ModelReadinessProvider
import com.kevin.astra.domain.settings.AiConfiguration
import com.kevin.astra.domain.settings.AiConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val aiConfigurationRepository: AiConfigurationRepository,
    private val modelCatalog: ModelCatalog,
    private val backendCatalog: BackendCatalog,
    private val modelReadinessProvider: ModelReadinessProvider,
    observationScope: CoroutineScope? = null,
) : AstraViewModel<SettingsState, SettingsIntent, SettingsEffect>(
    initialState = SettingsState(
        availableModels = modelCatalog.availableModels(),
        selectedModel = modelCatalog.currentModel(),
        modelReadiness = modelReadinessProvider.readinessFor(modelCatalog.availableModels()),
        availableBackends = backendCatalog.availableBackends(),
        selectedBackend = backendCatalog.currentBackend(),
    ),
) {
    private val settingsScope = observationScope ?: viewModelScope

    init {
        settingsScope.launch {
            aiConfigurationRepository.observeConfiguration().collect { configuration ->
                updateState {
                    configuration.toSettingsState(
                        modelCatalog = modelCatalog,
                        backendCatalog = backendCatalog,
                        modelReadinessProvider = modelReadinessProvider,
                    )
                }
            }
        }
    }

    override fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SelectModel -> {
                if (modelCatalog.selectModel(intent.modelId)) {
                    val selectedModel = modelCatalog.currentModel()
                    updateState { copy(selectedModel = selectedModel) }
                    settingsScope.launch {
                        aiConfigurationRepository.updateSelectedModel(selectedModel.id)
                    }
                }
            }

            is SettingsIntent.SelectBackend -> {
                if (backendCatalog.selectBackend(intent.backendId)) {
                    val selectedBackend = backendCatalog.currentBackend()
                    updateState { copy(selectedBackend = selectedBackend) }
                    settingsScope.launch {
                        aiConfigurationRepository.updateSelectedBackend(selectedBackend.id)
                    }
                }
            }

            is SettingsIntent.SelectIndustry -> {
                updateState { copy(selectedIndustry = intent.industry) }
                settingsScope.launch { aiConfigurationRepository.updateIndustry(intent.industry) }
            }

            is SettingsIntent.UpdateTemperature -> {
                updateState { copy(temperature = intent.temperature.coerceIn(0.0, 1.0)) }
                settingsScope.launch { aiConfigurationRepository.updateTemperature(intent.temperature) }
            }

            is SettingsIntent.UpdateMaxTokens -> {
                updateState { copy(maxTokens = intent.maxTokens.coerceIn(128, 2_048)) }
                settingsScope.launch { aiConfigurationRepository.updateMaxTokens(intent.maxTokens) }
            }

            is SettingsIntent.UpdateContextWindow -> {
                updateState { copy(contextWindow = intent.contextWindow.coerceIn(1_024, 32_768)) }
                settingsScope.launch { aiConfigurationRepository.updateContextWindow(intent.contextWindow) }
            }

            is SettingsIntent.UpdateQuantization -> {
                updateState { copy(quantization = intent.quantization) }
                settingsScope.launch { aiConfigurationRepository.updateQuantization(intent.quantization) }
            }

            is SettingsIntent.ToggleExperimentalFeatures -> {
                updateState { copy(experimentalFeaturesEnabled = intent.enabled) }
                settingsScope.launch { aiConfigurationRepository.updateExperimentalFeaturesEnabled(intent.enabled) }
            }
        }
    }
}

private fun AiConfiguration.toSettingsState(
    modelCatalog: ModelCatalog,
    backendCatalog: BackendCatalog,
    modelReadinessProvider: ModelReadinessProvider,
): SettingsState =
    SettingsState(
        availableModels = modelCatalog.availableModels(),
        selectedModel = modelCatalog.modelById(selectedModelId) ?: modelCatalog.currentModel(),
        modelReadiness = modelReadinessProvider.readinessFor(modelCatalog.availableModels()),
        availableBackends = backendCatalog.availableBackends(),
        selectedBackend = backendCatalog.backendById(selectedBackendId) ?: backendCatalog.currentBackend(),
        selectedIndustry = selectedIndustry,
        temperature = temperature,
        maxTokens = maxTokens,
        contextWindow = contextWindow,
        quantization = quantization,
        experimentalFeaturesEnabled = experimentalFeaturesEnabled,
    )
