package com.kevin.astra.presentation.demo

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.BackendStatus
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.device.DeviceCapabilityProvider
import com.kevin.astra.core.device.SupportedFeature
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.domain.demo.DemoScenarioCatalog
import com.kevin.astra.domain.modelmanager.ModelReadinessProvider
import com.kevin.astra.domain.modelmanager.ModelReadinessStatus
import com.kevin.astra.domain.settings.AiConfiguration
import com.kevin.astra.domain.settings.AiConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class DemoViewModel(
    private val deviceCapabilityProvider: DeviceCapabilityProvider,
    private val aiConfigurationRepository: AiConfigurationRepository,
    private val modelCatalog: ModelCatalog,
    private val backendCatalog: BackendCatalog,
    private val modelReadinessProvider: ModelReadinessProvider,
    private val demoScenarioCatalog: DemoScenarioCatalog,
    observationScope: CoroutineScope? = null,
) : AstraViewModel<DemoState, DemoIntent, DemoEffect>(
    initialState = DemoState(
        selectedModel = modelCatalog.currentModel(),
        selectedBackend = backendCatalog.currentBackend(),
        modelReadiness = modelReadinessProvider.readinessFor(modelCatalog.availableModels()),
    ),
) {
    private val demoScope = observationScope ?: viewModelScope

    init {
        refreshCapabilities()
        demoScope.launch {
            aiConfigurationRepository.observeConfiguration().collect { configuration ->
                updateState {
                    fromConfiguration(configuration).withReadiness()
                }
            }
        }
    }

    override fun handleIntent(intent: DemoIntent) {
        when (intent) {
            DemoIntent.Refresh -> refreshCapabilities()
            DemoIntent.NextStep -> moveStep(offset = 1)
            DemoIntent.PreviousStep -> moveStep(offset = -1)
            is DemoIntent.SelectStep -> updateState {
                copy(
                    currentStep = intent.step,
                    completedSteps = completedSteps + DemoStep.entries.takeWhile { it != intent.step },
                )
            }
        }
    }

    private fun refreshCapabilities() {
        demoScope.launch {
            updateState {
                copy(
                    isLoadingCapabilities = true,
                    error = null,
                ).withReadiness()
            }

            runCatching { deviceCapabilityProvider.getCapabilities() }
                .onSuccess { capabilities ->
                    updateState {
                        copy(
                            capabilities = capabilities,
                            isLoadingCapabilities = false,
                            error = null,
                        ).withReadiness()
                    }
                }
                .onFailure {
                    updateState {
                        copy(
                            isLoadingCapabilities = false,
                            error = "Unable to read demo device capabilities.",
                        ).withReadiness()
                    }
                }
        }
    }

    private fun moveStep(offset: Int) {
        updateState {
            val steps = DemoStep.entries
            val currentIndex = steps.indexOf(currentStep)
            val nextIndex = (currentIndex + offset).coerceIn(0, steps.lastIndex)
            val nextStep = steps[nextIndex]
            copy(
                currentStep = nextStep,
                completedSteps = if (offset > 0) {
                    completedSteps + currentStep
                } else {
                    completedSteps - currentStep
                },
            )
        }
    }

    private fun DemoState.fromConfiguration(configuration: AiConfiguration): DemoState =
        copy(
            selectedModel = modelCatalog.modelById(configuration.selectedModelId) ?: modelCatalog.currentModel(),
            selectedBackend = backendCatalog.backendById(configuration.selectedBackendId) ?: backendCatalog.currentBackend(),
            selectedIndustry = configuration.selectedIndustry,
            modelReadiness = modelReadinessProvider.readinessFor(modelCatalog.availableModels()),
        )

    private fun DemoState.withReadiness(): DemoState =
        copy(readinessIndicators = buildReadinessIndicators())

    private fun DemoState.buildReadinessIndicators(): List<DemoReadinessIndicator> {
        val capabilities = capabilities
        val selectedModelReadiness = modelReadiness.firstOrNull { it.modelId == selectedModel?.id }
        val deviceReady = capabilities?.supportedFeatures?.contains(SupportedFeature.OfflineMode) == true
        val runtimeReady = selectedBackend?.status == BackendStatus.Installed
        val modelReady = selectedModelReadiness?.status == ModelReadinessStatus.Installed
        val benchmarkReady = capabilities?.supportedFeatures?.contains(SupportedFeature.Benchmark) == true &&
            selectedModel != null &&
            selectedBackend != null
        val documentsReady = capabilities?.supportedFeatures?.contains(SupportedFeature.DocumentQA) == true &&
            demoScenarioCatalog.scenarios().isNotEmpty()

        return listOf(
            DemoReadinessIndicator(
                label = "Device Ready",
                status = when {
                    isLoadingCapabilities -> DemoReadinessStatus.Warning
                    deviceReady -> DemoReadinessStatus.Ready
                    else -> DemoReadinessStatus.Blocked
                },
                message = if (deviceReady) {
                    "${capabilities.platform} supports offline demo mode."
                } else {
                    "Device capability scan is pending or offline mode was not detected."
                },
            ),
            DemoReadinessIndicator(
                label = "Runtime Ready",
                status = if (runtimeReady) DemoReadinessStatus.Ready else DemoReadinessStatus.Warning,
                message = selectedBackend?.let { "${it.displayName} is ${it.status.label.lowercase()}." }
                    ?: "No runtime backend selected.",
            ),
            DemoReadinessIndicator(
                label = "Model Ready",
                status = if (modelReady) DemoReadinessStatus.Ready else DemoReadinessStatus.Warning,
                message = selectedModelReadiness?.readinessMessage
                    ?: "No model readiness metadata available.",
            ),
            DemoReadinessIndicator(
                label = "Benchmark Ready",
                status = if (benchmarkReady) DemoReadinessStatus.Ready else DemoReadinessStatus.Warning,
                message = if (benchmarkReady) {
                    "Benchmark lab can run with the selected model and backend."
                } else {
                    "Benchmark demo needs device support plus selected model/backend metadata."
                },
            ),
            DemoReadinessIndicator(
                label = "Documents Ready",
                status = if (documentsReady) DemoReadinessStatus.Ready else DemoReadinessStatus.Warning,
                message = if (documentsReady) {
                    "Embedded demo scenarios and document assistant flow are available."
                } else {
                    "Document Q&A demo is not fully available on this device."
                },
            ),
        )
    }
}
