package com.kevin.astra.domain.settings

import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.PromptIndustry

data class AiConfiguration(
    val selectedModel: AiModel = AiModel.Mock,
    val selectedBackend: InferenceBackend = InferenceBackend.Mock,
    val selectedIndustry: PromptIndustry = PromptIndustry.IndustrialMaintenance,
    val temperature: Double = 0.3,
    val maxTokens: Int = 512,
    val contextWindow: Int = 4_096,
    val quantization: String = "4-bit",
    val experimentalFeaturesEnabled: Boolean = false,
)
