package com.kevin.astra.presentation.benchmark

import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.data.ai.DefaultModelCatalog
import com.kevin.astra.domain.benchmark.BenchmarkRecommendation
import com.kevin.astra.domain.benchmark.BenchmarkReport
import com.kevin.astra.domain.benchmark.BenchmarkRequest
import com.kevin.astra.domain.benchmark.BenchmarkResult
import com.kevin.astra.domain.benchmark.BenchmarkRunner
import com.kevin.astra.domain.benchmark.BenchmarkStatus
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
    fun startsWithDefaultPromptAndThreeModels() {
        val viewModel = testViewModel()

        val state = viewModel.state.value

        assertEquals(DefaultBenchmarkPrompt, state.prompt)
        assertEquals(setOf("mock-model", "gemma-3-1b", "phi-3-mini"), state.selectedModelIds)
        assertEquals(5, state.availableModels.size)
        assertEquals(InferenceBackend.Mock, state.selectedBackend)
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
    fun runsBenchmarkAndStoresResults() = runBlocking {
        val viewModel = BenchmarkViewModel(
            benchmarkRunner = testRunner(),
            modelCatalog = DefaultModelCatalog(),
            benchmarkScope = CoroutineScope(coroutineContext),
        )

        viewModel.dispatch(BenchmarkIntent.RunBenchmark)
        yield()

        assertTrue(viewModel.state.value.isRunning)

        delay(20)

        val state = viewModel.state.value
        assertFalse(state.isRunning)
        assertEquals(3, state.results.size)
        assertEquals("gemma-3-1b", state.recommendedModel?.model?.id)
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

        assertEquals("Select at least one model and enter a benchmark prompt.", viewModel.state.value.error)
    }

    private fun testViewModel(): BenchmarkViewModel =
        BenchmarkViewModel(
            benchmarkRunner = testRunner(),
            modelCatalog = DefaultModelCatalog(),
        )

    private fun testRunner(): BenchmarkRunner =
        object : BenchmarkRunner {
            override suspend fun run(request: BenchmarkRequest): BenchmarkReport {
                delay(10)
                val gemma = request.models.modelById("gemma-3-1b")
                val results = request.models.mapIndexed { index, model ->
                    BenchmarkResult(
                        model = model,
                        backend = request.backend,
                        latencyMillis = 1_000L + index,
                        timeToFirstTokenMillis = 250L + index,
                        tokensPerSecond = 20 + index,
                        memoryUsageMb = 400 + index,
                        qualityScore = if (model.id == "gemma-3-1b") 92 else 80 + index,
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

private fun List<LocalModel>.modelById(id: String): LocalModel =
    first { it.id == id }
