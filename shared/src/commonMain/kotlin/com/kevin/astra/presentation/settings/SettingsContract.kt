package com.kevin.astra.presentation.settings

import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.core.mvi.AstraEffect
import com.kevin.astra.core.mvi.AstraIntent
import com.kevin.astra.core.mvi.AstraState

data class SettingsState(
    val availableModels: List<LocalModel> = emptyList(),
    val selectedModel: LocalModel? = null,
    val selectedBackend: InferenceBackend = InferenceBackend.Mock,
    val selectedIndustry: PromptIndustry = PromptIndustry.IndustrialMaintenance,
    val temperature: Double = 0.3,
    val maxTokens: Int = 512,
    val contextWindow: Int = 4_096,
    val quantization: String = "4-bit",
    val experimentalFeaturesEnabled: Boolean = false,
) : AstraState

sealed interface SettingsIntent : AstraIntent {
    data class SelectModel(val modelId: String) : SettingsIntent
    data class SelectBackend(val backend: InferenceBackend) : SettingsIntent
    data class SelectIndustry(val industry: PromptIndustry) : SettingsIntent
    data class UpdateTemperature(val temperature: Double) : SettingsIntent
    data class UpdateMaxTokens(val maxTokens: Int) : SettingsIntent
    data class UpdateContextWindow(val contextWindow: Int) : SettingsIntent
    data class UpdateQuantization(val quantization: String) : SettingsIntent
    data class ToggleExperimentalFeatures(val enabled: Boolean) : SettingsIntent
}

sealed interface SettingsEffect : AstraEffect
