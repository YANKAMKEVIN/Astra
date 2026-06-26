package com.kevin.astra.presentation.benchmark

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.ai.PromptBuildRequest
import com.kevin.astra.core.ai.PromptPipeline
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.domain.benchmark.BenchmarkRequest
import com.kevin.astra.domain.benchmark.BenchmarkRunner
import com.kevin.astra.domain.settings.AiConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class BenchmarkViewModel(
    private val benchmarkRunner: BenchmarkRunner,
    private val modelCatalog: ModelCatalog,
    private val backendCatalog: BackendCatalog,
    private val aiConfigurationRepository: AiConfigurationRepository,
    private val promptPipeline: PromptPipeline,
    private val benchmarkScope: CoroutineScope? = null,
) : AstraViewModel<BenchmarkState, BenchmarkIntent, BenchmarkEffect>(
    initialState = BenchmarkState(
        availableModels = modelCatalog.availableModels(),
        selectedModelIds = setOf(modelCatalog.currentModel().id),
        availableBackends = backendCatalog.availableBackends(),
        selectedBackend = backendCatalog.currentBackend(),
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

            BenchmarkIntent.RunBenchmark -> runBenchmark()
        }
    }

    private fun runBenchmark() {
        val snapshot = state.value
        if (!snapshot.canRun) {
            updateState {
                copy(error = "Select at least one model and enter a benchmark prompt.")
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

            val report = benchmarkRunner.run(
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
                    )
                },
            )

            updateState {
                copy(
                    isRunning = false,
                    results = report.results,
                    recommendedModel = report.recommendation,
                )
            }
        }
    }

    private fun BenchmarkState.selectedModels() =
        availableModels.filter { it.id in selectedModelIds }
}
