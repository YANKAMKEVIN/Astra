package com.kevin.astra.presentation.benchmark

import com.kevin.astra.core.ai.DefaultPromptBuilder
import com.kevin.astra.core.ai.DefaultPromptPipeline
import com.kevin.astra.core.ai.GenerationRuntimeInfo
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.core.ai.PromptPipeline
import com.kevin.astra.core.ai.RuntimeMode
import com.kevin.astra.core.navigation.AstraDestination
import com.kevin.astra.core.notification.NotificationService
import com.kevin.astra.data.ai.DefaultBackendCatalog
import com.kevin.astra.data.ai.DefaultModelCatalog
import com.kevin.astra.data.demo.StaticDemoScenarioCatalog
import com.kevin.astra.data.settings.testAiConfigurationRepository
import com.kevin.astra.domain.benchmark.BenchmarkRecommendation
import com.kevin.astra.domain.benchmark.BenchmarkReport
import com.kevin.astra.domain.benchmark.BenchmarkRequest
import com.kevin.astra.domain.benchmark.BenchmarkResult
import com.kevin.astra.domain.benchmark.BenchmarkRunner
import com.kevin.astra.domain.benchmark.BenchmarkStatus
import com.kevin.astra.domain.benchmark.HardwareSensorReader
import com.kevin.astra.domain.benchmark.HardwareSnapshot
import com.kevin.astra.domain.evaluation.RuleBasedTaskEvaluationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BenchmarkViewModelTest {
    @Test
    fun startsWithDefaultPromptAndPersistedModel() {
        val viewModel = testViewModel()

        val state = viewModel.state.value

        assertEquals(DefaultBenchmarkPrompt, state.prompt)
        assertEquals(setOf("mock-model"), state.selectedModelIds)
        assertEquals(5, state.availableModels.size)
        assertEquals(6, state.availableBackends.size)
        assertEquals("mock-engine", state.selectedBackend?.id)
        assertFalse(state.isRunning)
    }

    @Test
    fun togglesModelSelectionAndPrompt() {
        val viewModel = testViewModel()

        viewModel.dispatch(BenchmarkIntent.UpdatePrompt("Compare checklist answer"))
        viewModel.dispatch(BenchmarkIntent.ToggleModel("mock-model"))
        viewModel.dispatch(BenchmarkIntent.ToggleModel("qwen-2-5-1-5b"))

        val state = viewModel.state.value
        assertEquals("Compare checklist answer", state.prompt)
        assertFalse("mock-model" in state.selectedModelIds)
        assertTrue("qwen-2-5-1-5b" in state.selectedModelIds)
    }

    @Test
    fun selectAllAndClearAll() {
        val viewModel = testViewModel()

        viewModel.dispatch(BenchmarkIntent.SelectAllModels)
        assertEquals(5, viewModel.state.value.selectedModelIds.size)

        viewModel.dispatch(BenchmarkIntent.ClearModelSelection)
        assertTrue(viewModel.state.value.selectedModelIds.isEmpty())
    }

    @Test
    fun selectingScenarioPopulatesBenchmarkPrompt() {
        val catalog = StaticDemoScenarioCatalog()
        val scenario = catalog.scenarioById("aero-02") ?: error("Missing aerospace demo scenario")
        val viewModel = testViewModel(catalog = catalog)

        viewModel.dispatch(BenchmarkIntent.SelectScenario(scenario))

        assertEquals(scenario.prompt, viewModel.state.value.prompt)
        assertTrue(viewModel.state.value.canRun)
    }

    @Test
    fun runsBenchmarkAndStoresResults() = runBlocking {
        var capturedPrompt: String? = null
        val viewModel = testViewModel(
            runner = testRunner(onRequest = { capturedPrompt = it.prompt }),
            scope = CoroutineScope(coroutineContext),
        )

        viewModel.dispatch(BenchmarkIntent.ToggleModel("gemma-3-1b"))
        viewModel.dispatch(BenchmarkIntent.ToggleModel("phi-3-mini"))
        viewModel.dispatch(BenchmarkIntent.RunBenchmark)
        yield()

        assertTrue(viewModel.state.value.isRunning)

        delay(100)

        val state = viewModel.state.value
        assertFalse(state.isRunning)
        assertEquals(3, state.results.size)
        assertEquals("gemma-3-1b", state.recommendedModel?.model?.id)
        assertTrue(capturedPrompt.orEmpty().contains("System role"))
        assertTrue(capturedPrompt.orEmpty().contains(DefaultBenchmarkPrompt))
    }

    @Test
    fun hardwareSnapshotsAttachedToResults() = runBlocking {
        var snapshotCount = 0
        val viewModel = testViewModel(
            hardwareSensorReader = object : HardwareSensorReader {
                override fun read(): HardwareSnapshot {
                    snapshotCount++
                    return HardwareSnapshot(batteryPercent = 80 - snapshotCount, temperatureCelsius = 35f + snapshotCount, timestampMs = 0L)
                }
            },
            scope = CoroutineScope(coroutineContext),
        )

        viewModel.dispatch(BenchmarkIntent.RunBenchmark)
        delay(100)

        val result = viewModel.state.value.results.firstOrNull()
        assertTrue(result?.hardwareBefore != null)
        assertTrue(result?.hardwareAfter != null)
    }

    @Test
    fun showsErrorWhenNoModelSelected() {
        val viewModel = testViewModel()

        viewModel.state.value.availableModels.forEach { model ->
            if (model.id in viewModel.state.value.selectedModelIds) {
                viewModel.dispatch(BenchmarkIntent.ToggleModel(model.id))
            }
        }
        viewModel.dispatch(BenchmarkIntent.RunBenchmark)

        assertEquals("Select at least one model.", viewModel.state.value.error)
    }

    @Test
    fun cannotRunWithoutSelectedBackend() {
        val state = BenchmarkState(
            prompt = "Compare local inference options",
            selectedModelIds = setOf("mock-model"),
            selectedBackend = null,
        )
        assertFalse(state.canRun)
    }

    private fun testViewModel(
        runner: BenchmarkRunner = testRunner(),
        hardwareSensorReader: HardwareSensorReader = FakeHardwareSensorReader(),
        catalog: StaticDemoScenarioCatalog = StaticDemoScenarioCatalog(),
        scope: CoroutineScope? = null,
    ): BenchmarkViewModel =
        BenchmarkViewModel(
            benchmarkRunner = runner,
            hardwareSensorReader = hardwareSensorReader,
            modelCatalog = DefaultModelCatalog(),
            backendCatalog = DefaultBackendCatalog(),
            aiConfigurationRepository = testAiConfigurationRepository(),
            promptPipeline = DefaultPromptPipeline(DefaultPromptBuilder()),
            demoScenarioCatalog = catalog,
            notificationService = NoOpNotificationService(),
            benchmarkScope = scope,
        )

    private fun testRunner(onRequest: (BenchmarkRequest) -> Unit = {}): BenchmarkRunner =
        object : BenchmarkRunner {
            override suspend fun run(request: BenchmarkRequest): BenchmarkReport {
                onRequest(request)
                delay(10)
                val gemma = request.models.modelById("gemma-3-1b")
                val results = request.models.mapIndexed { index, model ->
                    BenchmarkResult(
                        model = model,
                        selectedBackend = request.backend,
                        usedBackend = request.backend,
                        runtimeInfo = GenerationRuntimeInfo(
                            mode = RuntimeMode.Simulated,
                            inferenceLatencyMillis = 1_000L + index,
                            totalExecutionTimeMillis = 1_100L + index,
                        ),
                        latencyMillis = 1_000L + index,
                        timeToFirstTokenMillis = 250L + index,
                        tokensPerSecond = 20 + index,
                        memoryUsageMb = 400 + index,
                        taskEvaluation = RuleBasedTaskEvaluationEngine().evaluate(
                            prompt = request.prompt,
                            response = if (model.id == "gemma-3-1b") {
                                "1. Verify safety. 2. Inspect pressure and shutdown cause. 3. Follow the checklist. 4. Restart and monitor vibration."
                            } else {
                                "Check the system and restart."
                            },
                            industry = PromptIndustry.IndustrialMaintenance,
                        ),
                        status = BenchmarkStatus.Simulated,
                    )
                }
                return BenchmarkReport(
                    results = results,
                    recommendation = BenchmarkRecommendation(
                        model = gemma,
                        explanation = "Gemma wins the simulated profile.",
                    ),
                )
            }
        }
}

private class FakeHardwareSensorReader : HardwareSensorReader {
    override fun read(): HardwareSnapshot = HardwareSnapshot(batteryPercent = 80, temperatureCelsius = 35f, timestampMs = 0L)
}

private class NoOpNotificationService : NotificationService {
    override fun showNotification(title: String, message: String, targetDestination: AstraDestination) = Unit
}

private fun List<LocalModel>.modelById(id: String): LocalModel = first { it.id == id }
