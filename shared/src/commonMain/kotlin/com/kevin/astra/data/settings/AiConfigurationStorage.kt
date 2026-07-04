package com.kevin.astra.data.settings

import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.BackendStatus
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.domain.settings.AiConfiguration
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
    private val backendCatalog: BackendCatalog,
    private val modelCatalog: ModelCatalog,
) {
    fun loadConfiguration(): AiConfiguration =
        AiConfiguration(
            selectedModelId = resolveSelectedModelId(keyValueStore.getString(SelectedModelIdKey)),
            selectedBackendId = resolveSelectedBackendId(keyValueStore.getString(SelectedBackendIdKey)),
            selectedIndustry = keyValueStore.getString(SelectedIndustryKey)
                ?.takeIf { it.isNotEmpty() }
                ?.let { saved -> PromptIndustry.entries.firstOrNull { it.name == saved } },
            temperature = keyValueStore.getDouble(TemperatureKey) ?: 0.3,
            maxTokens = keyValueStore.getInt(MaxTokensKey) ?: 512,
            contextWindow = keyValueStore.getInt(ContextWindowKey) ?: 4_096,
            quantization = keyValueStore.getString(QuantizationKey) ?: "4-bit",
            experimentalFeaturesEnabled = keyValueStore.getBoolean(ExperimentalFeaturesEnabledKey) ?: false,
            demoModeEnabled = keyValueStore.getBoolean(DemoModeEnabledKey) ?: false,
            lightThemeEnabled = keyValueStore.getBoolean(LightThemeEnabledKey) ?: false,
            huggingFaceToken = keyValueStore.getString(HuggingFaceTokenKey)?.takeIf { it.isNotEmpty() },
        )

    fun saveConfiguration(configuration: AiConfiguration) {
        keyValueStore.putString(SelectedModelIdKey, configuration.selectedModelId)
        keyValueStore.putString(SelectedBackendIdKey, configuration.selectedBackendId)
        keyValueStore.putString(SelectedIndustryKey, configuration.selectedIndustry?.name ?: "")
        keyValueStore.putDouble(TemperatureKey, configuration.temperature)
        keyValueStore.putInt(MaxTokensKey, configuration.maxTokens)
        keyValueStore.putInt(ContextWindowKey, configuration.contextWindow)
        keyValueStore.putString(QuantizationKey, configuration.quantization)
        keyValueStore.putBoolean(ExperimentalFeaturesEnabledKey, configuration.experimentalFeaturesEnabled)
        keyValueStore.putBoolean(DemoModeEnabledKey, configuration.demoModeEnabled)
        keyValueStore.putBoolean(LightThemeEnabledKey, configuration.lightThemeEnabled)
        keyValueStore.putString(HuggingFaceTokenKey, configuration.huggingFaceToken ?: "")
    }

    /**
     * Keeps the user's saved backend only while it is still installed/detected; otherwise (fresh
     * install, or a previously-selected backend that is no longer available) falls back to the
     * catalog's preferred default — the real LiteRT-LM runtime when ready, else the Mock engine.
     */
    private fun resolveSelectedBackendId(savedBackendId: String?): String {
        val savedIsUsable = savedBackendId
            ?.let { backendCatalog.backendById(it) }
            ?.status == BackendStatus.Installed
        return if (savedIsUsable) savedBackendId!! else backendCatalog.preferredDefaultBackend().id
    }

    /**
     * Keeps the user's saved model only while it is still in the catalog. A model that this platform
     * can't run is no longer listed, so a previously-saved selection of it falls back to the default
     * (Mock) instead of leaving the app pointing at a model that no longer exists.
     */
    private fun resolveSelectedModelId(savedModelId: String?): String =
        savedModelId?.takeIf { modelCatalog.modelById(it) != null } ?: DefaultSelectedModelId
}

private const val SelectedModelIdKey = "ai.selected_model_id"
private const val SelectedBackendIdKey = "ai.selected_backend_id"
private const val SelectedIndustryKey = "ai.selected_industry"
private const val TemperatureKey = "ai.temperature"
private const val MaxTokensKey = "ai.max_tokens"
private const val ContextWindowKey = "ai.context_window"
private const val QuantizationKey = "ai.quantization"
private const val ExperimentalFeaturesEnabledKey = "ai.experimental_features_enabled"
private const val DemoModeEnabledKey = "ai.demo_mode_enabled"
private const val LightThemeEnabledKey = "ai.light_theme_enabled"
private const val HuggingFaceTokenKey = "ai.hugging_face_token"
