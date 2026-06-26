package com.kevin.astra.data.benchmark

import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.data.ai.DefaultModelCatalog
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
        val models = DefaultModelCatalog().availableModels()

        val report = runner.run(
            BenchmarkRequest(
                prompt = "Restart Pump A",
                models = models.take(3),
                backend = InferenceBackend.Mock,
            ),
        )

        assertEquals(3, report.results.size)
        assertEquals(listOf("mock-model", "gemma-3-1b", "phi-3-mini"), report.results.map { it.model.id })
        assertTrue(report.results.all { it.backend == InferenceBackend.Mock })
    }

    @Test
    fun recommendsBestModelByQualityThenPerformance() = runBlocking {
        val runner = MockBenchmarkRunner()
        val models = DefaultModelCatalog().availableModels()

        val report = runner.run(
            BenchmarkRequest(
                prompt = "Restart Pump A",
                models = models,
                backend = InferenceBackend.Mock,
            ),
        )

        assertNotNull(report.recommendation)
        assertEquals("llama-3-2-3b", report.recommendation.model.id)
    }
}
