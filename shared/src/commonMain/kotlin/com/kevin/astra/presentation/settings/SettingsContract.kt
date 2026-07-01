package com.kevin.astra.presentation.settings

import com.kevin.astra.core.ai.InferenceBackendInfo
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.core.mvi.AstraEffect
import com.kevin.astra.core.mvi.AstraIntent
import com.kevin.astra.core.mvi.AstraState
import com.kevin.astra.domain.modelmanager.ModelDownloadState
import com.kevin.astra.domain.modelmanager.ModelReadiness

data class SettingsState(
    val availableModels: List<LocalModel> = emptyList(),
    val selectedModel: LocalModel? = null,
    val modelReadiness: List<ModelReadiness> = emptyList(),
    val availableBackends: List<InferenceBackendInfo> = emptyList(),
    val selectedBackend: InferenceBackendInfo? = null,
    val selectedIndustry: PromptIndustry? = null,
    val temperature: Double = 0.3,
    val maxTokens: Int = 512,
    val contextWindow: Int = 4_096,
    val quantization: String = "4-bit",
    val experimentalFeaturesEnabled: Boolean = false,
    val demoModeEnabled: Boolean = false,
    val lightThemeEnabled: Boolean = false,
    val downloadState: ModelDownloadState = ModelDownloadState.Idle,
    val storageUsageMb: Float = 0f,
) : AstraState

sealed interface SettingsIntent : AstraIntent {
    data class SelectModel(val modelId: String) : SettingsIntent
    data class SelectBackend(val backendId: String) : SettingsIntent
    data class SelectIndustry(val industry: PromptIndustry?) : SettingsIntent
    data class UpdateTemperature(val temperature: Double) : SettingsIntent
    data class UpdateMaxTokens(val maxTokens: Int) : SettingsIntent
    data class UpdateContextWindow(val contextWindow: Int) : SettingsIntent
    data class UpdateQuantization(val quantization: String) : SettingsIntent
    data class ToggleExperimentalFeatures(val enabled: Boolean) : SettingsIntent
    data class ToggleDemoMode(val enabled: Boolean) : SettingsIntent
    data class ToggleLightTheme(val enabled: Boolean) : SettingsIntent
    data class DownloadModel(val modelId: String) : SettingsIntent
    data class DeleteModel(val modelId: String) : SettingsIntent
    data class CancelDownload(val modelId: String) : SettingsIntent
}

sealed interface SettingsEffect : AstraEffect {
    data class DownloadCompleted(val modelName: String) : SettingsEffect
    data class ShowError(val message: String) : SettingsEffect
}
