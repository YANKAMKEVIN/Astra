package com.kevin.astra.presentation.benchmark

import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.InferenceBackend
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
        val viewModel = BenchmarkViewModel(benchmarkRunner = testRunner())

        val state = viewModel.state.value

        assertEquals(DefaultBenchmarkPrompt, state.prompt)
        assertEquals(setOf(AiModel.Mock, AiModel.Gemma, AiModel.Phi), state.selectedModels)
        assertEquals(InferenceBackend.Mock, state.selectedBackend)
        assertFalse(state.isRunning)
    }

    @Test
    fun togglesModelSelectionAndPrompt() {
        val viewModel = BenchmarkViewModel(benchmarkRunner = testRunner())

        viewModel.dispatch(BenchmarkIntent.UpdatePrompt("Compare checklist answer"))
        viewModel.dispatch(BenchmarkIntent.ToggleModel(AiModel.Mock))
        viewModel.dispatch(BenchmarkIntent.ToggleModel(AiModel.Qwen))

        val state = viewModel.state.value
        assertEquals("Compare checklist answer", state.prompt)
        assertFalse(AiModel.Mock in state.selectedModels)
        assertTrue(AiModel.Qwen in state.selectedModels)
    }

    @Test
    fun runsBenchmarkAndStoresResults() = runBlocking {
        val viewModel = BenchmarkViewModel(
            benchmarkRunner = testRunner(),
            benchmarkScope = CoroutineScope(coroutineContext),
        )

        viewModel.dispatch(BenchmarkIntent.RunBenchmark)
        yield()

        assertTrue(viewModel.state.value.isRunning)

        delay(20)

        val state = viewModel.state.value
        assertFalse(state.isRunning)
        assertEquals(3, state.results.size)
        assertEquals(AiModel.Gemma, state.recommendedModel?.model)
    }

    @Test
    fun showsErrorWhenNoModelSelected() {
        val viewModel = BenchmarkViewModel(benchmarkRunner = testRunner())

        AiModel.entries.forEach { model ->
            if (model in viewModel.state.value.selectedModels) {
                viewModel.dispatch(BenchmarkIntent.ToggleModel(model))
            }
        }
        viewModel.dispatch(BenchmarkIntent.RunBenchmark)

        assertEquals("Select at least one model and enter a benchmark prompt.", viewModel.state.value.error)
    }

    private fun testRunner(): BenchmarkRunner =
        object : BenchmarkRunner {
            override suspend fun run(request: BenchmarkRequest): BenchmarkReport {
                delay(10)
                val results = request.models.mapIndexed { index, model ->
                    BenchmarkResult(
                        model = model,
                        backend = request.backend,
                        latencyMillis = 1_000L + index,
                        timeToFirstTokenMillis = 250L + index,
                        tokensPerSecond = 20 + index,
                        memoryUsageMb = 400 + index,
                        qualityScore = if (model == AiModel.Gemma) 92 else 80 + index,
                        status = BenchmarkStatus.Simulated,
                    )
                }
                return BenchmarkReport(
                    results = results,
                    recommendation = BenchmarkRecommendation(
                        model = AiModel.Gemma,
                        explanation = "Gemma wins the simulated profile.",
                    ),
                )
            }
        }
}
