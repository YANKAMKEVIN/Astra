package com.kevin.astra.data.benchmark

import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.domain.benchmark.BenchmarkRequest
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MockBenchmarkRunnerTest {
    @Test
    fun producesResultForEachSelectedModel() = runBlocking {
        val runner = MockBenchmarkRunner()

        val report = runner.run(
            BenchmarkRequest(
                prompt = "Restart Pump A",
                models = listOf(AiModel.Mock, AiModel.Gemma, AiModel.Phi),
                backend = InferenceBackend.Mock,
            ),
        )

        assertEquals(3, report.results.size)
        assertEquals(listOf(AiModel.Mock, AiModel.Gemma, AiModel.Phi), report.results.map { it.model })
        assertTrue(report.results.all { it.backend == InferenceBackend.Mock })
    }

    @Test
    fun recommendsBestModelByQualityThenPerformance() = runBlocking {
        val runner = MockBenchmarkRunner()

        val report = runner.run(
            BenchmarkRequest(
                prompt = "Restart Pump A",
                models = AiModel.entries,
                backend = InferenceBackend.Mock,
            ),
        )

        assertNotNull(report.recommendation)
        assertEquals(AiModel.Llama, report.recommendation.model)
    }
}
