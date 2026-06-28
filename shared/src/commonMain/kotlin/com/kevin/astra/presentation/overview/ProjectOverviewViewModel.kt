package com.kevin.astra.presentation.overview

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.BackendStatus
import com.kevin.astra.core.ai.InferenceBackendInfo
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.ai.ModelStatus
import com.kevin.astra.core.device.DeviceCapabilityProvider
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.domain.modelmanager.ModelReadinessProvider
import com.kevin.astra.domain.modelmanager.ModelReadinessStatus
import com.kevin.astra.domain.settings.AiConfiguration
import com.kevin.astra.domain.settings.AiConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ProjectOverviewViewModel(
    private val deviceCapabilityProvider: DeviceCapabilityProvider,
    private val aiConfigurationRepository: AiConfigurationRepository,
    private val modelCatalog: ModelCatalog,
    private val backendCatalog: BackendCatalog,
    private val modelReadinessProvider: ModelReadinessProvider,
    observationScope: CoroutineScope? = null,
) : AstraViewModel<ProjectOverviewState, ProjectOverviewIntent, ProjectOverviewEffect>(
    initialState = ProjectOverviewState(
        selectedBackend = backendCatalog.currentBackend(),
        selectedModel = modelCatalog.currentModel(),
        availableModels = modelCatalog.availableModels(),
        installedModels = modelCatalog.installedModels(),
        modelReadiness = modelReadinessProvider.readinessFor(modelCatalog.availableModels()),
        currentRuntime = backendCatalog.currentBackend().runtimeBackend.label,
        fallbackStatus = fallbackStatusFor(
            backend = backendCatalog.currentBackend(),
            modelStatus = modelCatalog.currentModel().status,
        ),
        architectureItems = architectureItems,
        aiFeatures = aiFeatures,
        documentationLinks = documentationLinks,
    ),
) {
    private val overviewScope = observationScope ?: viewModelScope

    init {
        refreshCapabilities()
        overviewScope.launch {
            aiConfigurationRepository.observeConfiguration().collect { configuration ->
                updateState { fromConfiguration(configuration) }
            }
        }
    }

    override fun handleIntent(intent: ProjectOverviewIntent) {
        when (intent) {
            ProjectOverviewIntent.Refresh -> refreshCapabilities()
        }
    }

    private fun refreshCapabilities() {
        overviewScope.launch {
            updateState {
                copy(
                    isLoadingCapabilities = true,
                    error = null,
                )
            }

            runCatching { deviceCapabilityProvider.getCapabilities() }
                .onSuccess { capabilities ->
                    updateState {
                        copy(
                            capabilities = capabilities,
                            isLoadingCapabilities = false,
                            error = null,
                        )
                    }
                }
                .onFailure {
                    updateState {
                        copy(
                            isLoadingCapabilities = false,
                            error = "Unable to load device overview.",
                        )
                    }
                }
        }
    }

    private fun ProjectOverviewState.fromConfiguration(configuration: AiConfiguration): ProjectOverviewState {
        val model = modelCatalog.modelById(configuration.selectedModelId) ?: modelCatalog.currentModel()
        val backend = backendCatalog.backendById(configuration.selectedBackendId) ?: backendCatalog.currentBackend()
        val readiness = modelReadinessProvider.readinessFor(modelCatalog.availableModels())
        val selectedReadiness = readiness.firstOrNull { it.modelId == model.id }

        return copy(
            selectedBackend = backend,
            selectedModel = model,
            availableModels = modelCatalog.availableModels(),
            installedModels = modelCatalog.installedModels(),
            modelReadiness = readiness,
            selectedIndustry = configuration.selectedIndustry,
            currentRuntime = backend.runtimeBackend.label,
            fallbackStatus = when {
                backend.status != BackendStatus.Installed -> "Fallback active: ${backend.status.label}"
                selectedReadiness?.status != ModelReadinessStatus.Installed -> "Fallback active: ${selectedReadiness?.status?.label ?: "model metadata unavailable"}"
                else -> "No fallback required for selected Mock runtime"
            },
        )
    }

}

private fun fallbackStatusFor(
    backend: InferenceBackendInfo,
    modelStatus: ModelStatus,
): String =
    when {
        backend.status != BackendStatus.Installed -> "Fallback active: ${backend.status.label}"
        modelStatus != ModelStatus.Installed -> "Fallback active: ${modelStatus.label}"
        else -> "No fallback required for selected Mock runtime"
    }

private val architectureItems = listOf(
    OverviewArchitectureItem("Clean Architecture", "Presentation, domain, data and platform runtime concerns remain separated."),
    OverviewArchitectureItem("MVI", "Screens expose immutable state, typed intents and predictable ViewModel reducers."),
    OverviewArchitectureItem("Kotlin Multiplatform", "Shared business logic and UI target Android and iOS from one codebase."),
    OverviewArchitectureItem("Koin", "Dependency injection wires catalogs, repositories, runtimes and ViewModels."),
    OverviewArchitectureItem("Prompt Pipeline", "Prompts are normalized through reusable industry-aware builders before inference."),
    OverviewArchitectureItem("RoutingInferenceEngine", "Runtime requests are routed to Mock, LiteRT or LiteRT-LM with transparent fallback."),
)

private val aiFeatures = listOf(
    "Assistant",
    "Benchmark",
    "Documents",
    "Task Evaluation",
    "Model Manager",
)

private val documentationLinks = listOf(
    OverviewDocumentationLink("README", "README.md", "Product overview, setup and project story."),
    OverviewDocumentationLink("Architecture", "docs/03_Platform_Architecture.md", "Platform layers, module boundaries and runtime design."),
    OverviewDocumentationLink("Benchmark Methodology", "docs/08_Benchmark_Methodology.md", "Benchmark scenarios, metrics and interpretation."),
    OverviewDocumentationLink("LiteRT Evaluation", "docs/06_LiteRT_LM_Evaluation.md", "LiteRT-LM feasibility notes and integration constraints."),
)
