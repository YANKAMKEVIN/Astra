package com.kevin.astra.presentation.settings

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.ai.ModelStatus
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.domain.modelmanager.ModelDownloadManager
import com.kevin.astra.domain.modelmanager.ModelDownloadRequest
import com.kevin.astra.domain.modelmanager.ModelDownloadState
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
    private val modelDownloadManager: ModelDownloadManager,
    observationScope: CoroutineScope? = null,
) : AstraViewModel<SettingsState, SettingsIntent, SettingsEffect>(
    initialState = SettingsState(
        availableModels = modelCatalog.availableModels(),
        selectedModel = modelCatalog.currentModel(),
        modelReadiness = modelReadinessProvider.readinessFor(modelCatalog.availableModels()),
        availableBackends = backendCatalog.availableBackends(),
        selectedBackend = backendCatalog.currentBackend(),
        storageUsageMb = modelDownloadManager.getStorageUsageMb(),
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
                        downloadState = downloadState,
                        storageUsageMb = storageUsageMb,
                    )
                }
            }
        }
        settingsScope.launch {
            modelDownloadManager.downloadState.collect { dlState ->
                when (dlState) {
                    is ModelDownloadState.Completed -> {
                        modelCatalog.updateModelStatus(dlState.modelId, ModelStatus.Installed)
                        val modelName = modelCatalog.modelById(dlState.modelId)?.displayName ?: dlState.modelId
                        refreshModels(dlState)
                        emitEffect(SettingsEffect.DownloadCompleted(modelName))
                    }
                    is ModelDownloadState.Failed -> {
                        refreshModels(dlState)
                        emitEffect(SettingsEffect.ShowError("Download failed for ${dlState.modelId}: ${dlState.reason}"))
                    }
                    else -> refreshModels(dlState)
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

            is SettingsIntent.ToggleDemoMode -> {
                updateState { copy(demoModeEnabled = intent.enabled) }
                settingsScope.launch { aiConfigurationRepository.updateDemoModeEnabled(intent.enabled) }
            }

            is SettingsIntent.ToggleLightTheme -> {
                updateState { copy(lightThemeEnabled = intent.enabled) }
                settingsScope.launch { aiConfigurationRepository.updateLightThemeEnabled(intent.enabled) }
            }

            is SettingsIntent.DownloadModel -> {
                val model = modelCatalog.modelById(intent.modelId) ?: return
                val url = model.downloadUrl ?: run {
                    settingsScope.launch { emitEffect(SettingsEffect.ShowError("No download URL for ${model.displayName}.")) }
                    return
                }
                val fileName = url.substringAfterLast('/')
                val token = state.value.huggingFaceToken.ifBlank { null }
                settingsScope.launch {
                    modelDownloadManager.download(
                        ModelDownloadRequest(
                            modelId = intent.modelId,
                            displayName = model.displayName,
                            url = url,
                            fileName = fileName,
                            authToken = token,
                        ),
                    )
                }
            }

            is SettingsIntent.DeleteModel -> {
                val deleted = modelDownloadManager.deleteModel(intent.modelId)
                if (deleted) {
                    modelCatalog.updateModelStatus(intent.modelId, ModelStatus.DownloadRequired)
                    updateState {
                        copy(
                            availableModels = modelCatalog.availableModels(),
                            modelReadiness = modelReadinessProvider.readinessFor(modelCatalog.availableModels()),
                            storageUsageMb = modelDownloadManager.getStorageUsageMb(),
                        )
                    }
                }
            }

            is SettingsIntent.UpdateHuggingFaceToken -> {
                updateState { copy(huggingFaceToken = intent.token) }
                settingsScope.launch {
                    aiConfigurationRepository.updateConfiguration(
                        aiConfigurationRepository.getConfiguration().copy(huggingFaceToken = intent.token.ifBlank { null }),
                    )
                }
            }

            is SettingsIntent.CancelDownload -> {
                modelDownloadManager.cancel(intent.modelId)
            }
        }
    }

    private fun refreshModels(dlState: ModelDownloadState) {
        updateState {
            copy(
                availableModels = modelCatalog.availableModels(),
                modelReadiness = modelReadinessProvider.readinessFor(modelCatalog.availableModels()),
                downloadState = dlState,
                storageUsageMb = modelDownloadManager.getStorageUsageMb(),
            )
        }
    }
}

private fun AiConfiguration.toSettingsState(
    modelCatalog: ModelCatalog,
    backendCatalog: BackendCatalog,
    modelReadinessProvider: ModelReadinessProvider,
    downloadState: ModelDownloadState,
    storageUsageMb: Float,
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
        demoModeEnabled = demoModeEnabled,
        lightThemeEnabled = lightThemeEnabled,
        huggingFaceToken = huggingFaceToken ?: "",
        downloadState = downloadState,
        storageUsageMb = storageUsageMb,
    )
