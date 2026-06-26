package com.kevin.astra.data.settings

import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.domain.settings.AiConfiguration
import com.kevin.astra.domain.settings.DefaultSelectedBackendId
import com.kevin.astra.domain.settings.DefaultSelectedModelId

interface AiConfigurationKeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getDouble(key: String): Double?
    fun putDouble(key: String, value: Double)
    fun getInt(key: String): Int?
    fun putInt(key: String, value: Int)
    fun getBoolean(key: String): Boolean?
    fun putBoolean(key: String, value: Boolean)
}

expect fun createAiConfigurationKeyValueStore(): AiConfigurationKeyValueStore

class AiConfigurationLocalDataSource(
    private val keyValueStore: AiConfigurationKeyValueStore,
) {
    fun loadConfiguration(): AiConfiguration =
        AiConfiguration(
            selectedModelId = keyValueStore.getString(SelectedModelIdKey) ?: DefaultSelectedModelId,
            selectedBackendId = keyValueStore.getString(SelectedBackendIdKey) ?: DefaultSelectedBackendId,
            selectedIndustry = keyValueStore.getString(SelectedIndustryKey)
                ?.let { saved -> PromptIndustry.entries.firstOrNull { it.name == saved } }
                ?: PromptIndustry.IndustrialMaintenance,
            temperature = keyValueStore.getDouble(TemperatureKey) ?: 0.3,
            maxTokens = keyValueStore.getInt(MaxTokensKey) ?: 512,
            contextWindow = keyValueStore.getInt(ContextWindowKey) ?: 4_096,
            quantization = keyValueStore.getString(QuantizationKey) ?: "4-bit",
            experimentalFeaturesEnabled = keyValueStore.getBoolean(ExperimentalFeaturesEnabledKey) ?: false,
        )

    fun saveConfiguration(configuration: AiConfiguration) {
        keyValueStore.putString(SelectedModelIdKey, configuration.selectedModelId)
        keyValueStore.putString(SelectedBackendIdKey, configuration.selectedBackendId)
        keyValueStore.putString(SelectedIndustryKey, configuration.selectedIndustry.name)
        keyValueStore.putDouble(TemperatureKey, configuration.temperature)
        keyValueStore.putInt(MaxTokensKey, configuration.maxTokens)
        keyValueStore.putInt(ContextWindowKey, configuration.contextWindow)
        keyValueStore.putString(QuantizationKey, configuration.quantization)
        keyValueStore.putBoolean(ExperimentalFeaturesEnabledKey, configuration.experimentalFeaturesEnabled)
    }
}

private const val SelectedModelIdKey = "ai.selected_model_id"
private const val SelectedBackendIdKey = "ai.selected_backend_id"
private const val SelectedIndustryKey = "ai.selected_industry"
private const val TemperatureKey = "ai.temperature"
private const val MaxTokensKey = "ai.max_tokens"
private const val ContextWindowKey = "ai.context_window"
private const val QuantizationKey = "ai.quantization"
private const val ExperimentalFeaturesEnabledKey = "ai.experimental_features_enabled"
