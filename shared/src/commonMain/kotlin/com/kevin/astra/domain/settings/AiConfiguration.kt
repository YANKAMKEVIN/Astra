package com.kevin.astra.domain.settings

import com.kevin.astra.core.ai.HardwareAcceleration
import com.kevin.astra.core.ai.PromptIndustry

data class AiConfiguration(
    val selectedModelId: String = DefaultSelectedModelId,
    val selectedBackendId: String = DefaultSelectedBackendId,
    val selectedIndustry: PromptIndustry? = null,
    val temperature: Double = 0.3,
    val maxTokens: Int = 512,
    val contextWindow: Int = 4_096,
    val quantization: String = "4-bit",
    val experimentalFeaturesEnabled: Boolean = false,
    val demoModeEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.Dark,
    val huggingFaceToken: String? = null,
    /** LiteRT-LM accelerator; GPU/NPU are opt-in until validated on device (fallback to CPU). */
    val hardwareAcceleration: HardwareAcceleration = HardwareAcceleration.Cpu,
)

const val DefaultSelectedModelId: String = "mock-model"
const val DefaultSelectedBackendId: String = "mock-engine"
