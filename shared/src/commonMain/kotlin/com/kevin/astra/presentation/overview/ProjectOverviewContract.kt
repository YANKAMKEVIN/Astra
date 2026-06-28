package com.kevin.astra.presentation.overview

import com.kevin.astra.core.ai.InferenceBackendInfo
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.core.device.DeviceCapabilities
import com.kevin.astra.core.mvi.AstraEffect
import com.kevin.astra.core.mvi.AstraIntent
import com.kevin.astra.core.mvi.AstraState
import com.kevin.astra.domain.modelmanager.ModelReadiness

data class OverviewArchitectureItem(
    val title: String,
    val description: String,
)

data class OverviewDocumentationLink(
    val title: String,
    val path: String,
    val description: String,
)

data class ProjectOverviewState(
    val capabilities: DeviceCapabilities? = null,
    val isLoadingCapabilities: Boolean = true,
    val selectedBackend: InferenceBackendInfo? = null,
    val selectedModel: LocalModel? = null,
    val availableModels: List<LocalModel> = emptyList(),
    val installedModels: List<LocalModel> = emptyList(),
    val modelReadiness: List<ModelReadiness> = emptyList(),
    val selectedIndustry: PromptIndustry = PromptIndustry.IndustrialMaintenance,
    val currentRuntime: String = "Unknown",
    val fallbackStatus: String = "Unknown",
    val architectureItems: List<OverviewArchitectureItem> = emptyList(),
    val aiFeatures: List<String> = emptyList(),
    val documentationLinks: List<OverviewDocumentationLink> = emptyList(),
    val error: String? = null,
) : AstraState

sealed interface ProjectOverviewIntent : AstraIntent {
    data object Refresh : ProjectOverviewIntent
}

sealed interface ProjectOverviewEffect : AstraEffect
