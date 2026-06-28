package com.kevin.astra.data.benchmark

import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.PromptIndustry
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
                industry = PromptIndustry.IndustrialMaintenance,
            ),
        )

        assertEquals(3, report.results.size)
        assertEquals(listOf("mock-model", "gemma-3-1b", "phi-3-mini"), report.results.map { it.model.id })
        assertTrue(report.results.all { it.backend == InferenceBackend.Mock })
        assertTrue(report.results.all { it.taskEvaluation.overallScore in 0..100 })
    }

    @Test
    fun recommendsBestModelByTaskEvaluationThenPerformance() = runBlocking {
        val runner = MockBenchmarkRunner()
        val models = DefaultModelCatalog().availableModels()

        val report = runner.run(
            BenchmarkRequest(
                prompt = "Restart Pump A",
                models = models,
                backend = InferenceBackend.Mock,
                industry = PromptIndustry.IndustrialMaintenance,
            ),
        )

        assertNotNull(report.recommendation)
        assertEquals(report.results.maxOf { it.taskEvaluation.overallScore }, report.recommendation.model.let { model ->
            report.results.first { it.model.id == model.id }.taskEvaluation.overallScore
        })
    }
}
