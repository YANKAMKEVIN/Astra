package com.kevin.astra.presentation.benchmark

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.ai.PromptBuildRequest
import com.kevin.astra.core.ai.PromptPipeline
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.core.navigation.AstraDestination
import com.kevin.astra.core.notification.NotificationService
import com.kevin.astra.domain.benchmark.BenchmarkRecommendation
import com.kevin.astra.domain.benchmark.BenchmarkRequest
import com.kevin.astra.domain.benchmark.BenchmarkResult
import com.kevin.astra.domain.benchmark.BenchmarkRunner
import com.kevin.astra.domain.benchmark.HardwareSensorReader
import com.kevin.astra.domain.demo.DemoScenarioCatalog
import com.kevin.astra.domain.settings.AiConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BenchmarkViewModel(
    private val benchmarkRunner: BenchmarkRunner,
    private val hardwareSensorReader: HardwareSensorReader,
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
            is BenchmarkIntent.UpdatePrompt -> updateState { copy(prompt = intent.prompt, error = null) }

            is BenchmarkIntent.ToggleModel -> updateState {
                val next = if (intent.modelId in selectedModelIds) selectedModelIds - intent.modelId
                           else selectedModelIds + intent.modelId
                copy(selectedModelIds = next, error = null)
            }

            is BenchmarkIntent.SelectBackend -> {
                if (backendCatalog.selectBackend(intent.backendId)) {
                    updateState { copy(selectedBackend = backendCatalog.currentBackend(), error = null) }
                }
            }

            is BenchmarkIntent.SelectScenario -> updateState { copy(prompt = intent.scenario.prompt, error = null) }

            BenchmarkIntent.SelectAllModels -> updateState {
                copy(selectedModelIds = availableModels.map { it.id }.toSet(), error = null)
            }

            BenchmarkIntent.ClearModelSelection -> updateState { copy(selectedModelIds = emptySet(), error = null) }

            BenchmarkIntent.RunBenchmark -> runBenchmark()
        }
    }

    private fun runBenchmark() {
        val snapshot = state.value
        if (!snapshot.canRun) {
            val message = when {
                snapshot.prompt.isBlank() -> "Enter a benchmark prompt before running."
                snapshot.selectedModelIds.isEmpty() -> "Select at least one model."
                snapshot.selectedBackend == null -> "Select a backend."
                else -> "Benchmark is already running."
            }
            updateState { copy(error = message) }
            return
        }

        (benchmarkScope ?: viewModelScope).launch {
            val configuration = aiConfigurationRepository.getConfiguration()
            val modelsToRun = snapshot.availableModels.filter { it.id in snapshot.selectedModelIds }
            val backendRuntime = snapshot.selectedBackend?.runtimeBackend
                ?: backendCatalog.currentBackend().runtimeBackend

            val preparedPrompt = withContext(Dispatchers.Default) {
                promptPipeline.preparePrompt(
                    PromptBuildRequest(
                        engineerQuestion = snapshot.prompt,
                        selectedIndustry = configuration.selectedIndustry,
                        selectedModel = modelsToRun.firstOrNull() ?: modelCatalog.currentModel(),
                    ),
                )
            }

            updateState {
                copy(
                    isRunning = true,
                    error = null,
                    results = emptyList(),
                    recommendedModel = null,
                    completedCount = 0,
                    currentlyBenchmarkingModel = modelsToRun.firstOrNull()?.displayName,
                )
            }

            val accumulatedResults = mutableListOf<BenchmarkResult>()

            modelsToRun.forEachIndexed { index, model ->
                updateState {
                    copy(currentlyBenchmarkingModel = model.displayName, completedCount = index)
                }

                val hardwareBefore = withContext(Dispatchers.Default) { hardwareSensorReader.read() }

                val report = withContext(Dispatchers.Default) {
                    benchmarkRunner.run(
                        BenchmarkRequest(
                            prompt = preparedPrompt,
                            models = listOf(model),
                            backend = backendRuntime,
                            industry = configuration.selectedIndustry,
                        ),
                    )
                }

                val hardwareAfter = withContext(Dispatchers.Default) { hardwareSensorReader.read() }

                val resultWithHardware = report.results.firstOrNull()?.copy(
                    hardwareBefore = hardwareBefore,
                    hardwareAfter = hardwareAfter,
                )
                if (resultWithHardware != null) accumulatedResults += resultWithHardware

                updateState { copy(results = accumulatedResults.toList(), completedCount = index + 1) }
            }

            val recommended = accumulatedResults
                .sortedWith(
                    compareByDescending<BenchmarkResult> { it.taskEvaluation.overallScore }
                        .thenBy { it.latencyMillis ?: Long.MAX_VALUE },
                )
                .firstOrNull()
                ?.let {
                    BenchmarkRecommendation(
                        model = it.model,
                        explanation = "${it.model.displayName} leads with task score ${it.taskEvaluation.overallScore}/100.",
                    )
                }

            updateState {
                copy(
                    isRunning = false,
                    currentlyBenchmarkingModel = null,
                    results = accumulatedResults,
                    recommendedModel = recommended,
                )
            }

            notificationService.showNotification(
                title = "Benchmark Complete",
                message = "Evaluated ${accumulatedResults.size} model(s). ${recommended?.model?.displayName ?: "See results"}.",
                targetDestination = AstraDestination.Benchmark,
            )
        }
    }
}
