package com.kevin.astra.presentation.benchmark

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.ai.PromptBuildRequest
import com.kevin.astra.core.ai.PromptPipeline
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.core.navigation.AstraDestination
import com.kevin.astra.core.notification.NotificationService
import com.kevin.astra.domain.benchmark.BenchmarkRequest
import com.kevin.astra.domain.benchmark.BenchmarkRunner
import com.kevin.astra.domain.demo.DemoScenarioCatalog
import com.kevin.astra.domain.settings.AiConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BenchmarkViewModel(
    private val benchmarkRunner: BenchmarkRunner,
    private val modelCatalog: ModelCatalog,
    private val backendCatalog: BackendCatalog,
    private val aiConfigurationRepository: AiConfigurationRepository,
    private val promptPipeline: PromptPipeline,
    private val demoScenarioCatalog: DemoScenarioCatalog,
    private val notificationService: NotificationService,
    private val benchmarkScope: CoroutineScope? = null,
) : AstraViewModel<BenchmarkState, BenchmarkIntent, BenchmarkEffect>(
    initialState = BenchmarkState(
        availableModels = modelCatalog.availableModels(),
        selectedModelIds = setOf(modelCatalog.currentModel().id),
        availableBackends = backendCatalog.availableBackends(),
        selectedBackend = backendCatalog.currentBackend(),
        availableScenarios = demoScenarioCatalog.scenarios(),
    ),
) {
    override fun handleIntent(intent: BenchmarkIntent) {
        when (intent) {
            is BenchmarkIntent.UpdatePrompt -> updateState {
                copy(prompt = intent.prompt, error = null)
            }

            is BenchmarkIntent.ToggleModel -> updateState {
                val nextModels = if (intent.modelId in selectedModelIds) {
                    selectedModelIds - intent.modelId
                } else {
                    selectedModelIds + intent.modelId
                }
                copy(selectedModelIds = nextModels, error = null)
            }

            is BenchmarkIntent.SelectBackend -> {
                if (backendCatalog.selectBackend(intent.backendId)) {
                    updateState {
                        copy(
                            selectedBackend = backendCatalog.currentBackend(),
                            error = null,
                        )
                    }
                }
            }

            is BenchmarkIntent.SelectScenario -> updateState {
                copy(prompt = intent.scenario.prompt, error = null)
            }

            BenchmarkIntent.RunBenchmark -> runBenchmark()
        }
    }

    private fun runBenchmark() {
        val snapshot = state.value
        if (!snapshot.canRun) {
            val message = when {
                snapshot.prompt.isBlank() -> "Enter a benchmark prompt before running ASTRA."
                snapshot.selectedModelIds.isEmpty() -> "Select at least one model before running ASTRA."
                snapshot.selectedBackend == null -> "Select an installed backend before running ASTRA."
                else -> "Benchmark is already running."
            }
            updateState {
                copy(error = message)
            }
            return
        }

        (benchmarkScope ?: viewModelScope).launch {
            updateState {
                copy(
                    isRunning = true,
                    error = null,
                    results = emptyList(),
                    recommendedModel = null,
                )
            }

            val report = withContext(Dispatchers.Default) {
                benchmarkRunner.run(
                    aiConfigurationRepository.getConfiguration().let { configuration ->
                        val persistedModel = modelCatalog.modelById(configuration.selectedModelId) ?: modelCatalog.currentModel()
                        val persistedBackend = backendCatalog.backendById(configuration.selectedBackendId) ?: backendCatalog.currentBackend()
                        val selectedModels = snapshot.selectedModels().ifEmpty { listOf(persistedModel) }

                        BenchmarkRequest(
                            prompt = promptPipeline.preparePrompt(
                                PromptBuildRequest(
                                    engineerQuestion = snapshot.prompt,
                                    selectedIndustry = configuration.selectedIndustry,
                                    selectedModel = selectedModels.first(),
                                ),
                            ),
                            models = selectedModels,
                            backend = snapshot.selectedBackend?.runtimeBackend ?: persistedBackend.runtimeBackend,
                            industry = configuration.selectedIndustry,
                        )
                    },
                )
            }

            updateState {
                copy(
                    isRunning = false,
                    results = report.results,
                    recommendedModel = report.recommendation,
                )
            }

            notificationService.showNotification(
                title = "Benchmark Complete",
                message = "ASTRA has finished evaluating all selected AI models.",
                targetDestination = AstraDestination.Benchmark
            )
        }
    }

    private fun BenchmarkState.selectedModels() =
        availableModels.filter { it.id in selectedModelIds }
}
