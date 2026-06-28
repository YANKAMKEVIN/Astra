package com.kevin.astra.data.benchmark

import com.kevin.astra.core.ai.GenerationMetrics
import com.kevin.astra.core.ai.GenerationResult
import com.kevin.astra.core.ai.GenerationRuntimeInfo
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.InferenceEngine
import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.core.ai.PromptRequest
import com.kevin.astra.core.ai.RuntimeMode
import com.kevin.astra.data.ai.DefaultModelCatalog
import com.kevin.astra.domain.benchmark.BenchmarkRequest
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RuntimeBenchmarkRunnerTest {
    @Test
    fun producesResultForEachSelectedModelUsingInferenceEngineMetrics() = runBlocking {
        val runner = RuntimeBenchmarkRunner(inferenceEngine = testInferenceEngine())
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
        assertTrue(report.results.all { it.selectedBackend == InferenceBackend.Mock })
        assertTrue(report.results.all { it.usedBackend == InferenceBackend.Mock })
        assertTrue(report.results.all { it.latencyMillis == 900L })
        assertTrue(report.results.all { it.taskEvaluation.overallScore in 0..100 })
    }

    @Test
    fun recommendsBestModelByTaskEvaluationThenRuntimePreference() = runBlocking {
        val runner = RuntimeBenchmarkRunner(inferenceEngine = testInferenceEngine())
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

    private fun testInferenceEngine(): InferenceEngine =
        object : InferenceEngine {
            override suspend fun generate(request: PromptRequest): GenerationResult =
                GenerationResult(
                    text = """
                        1. Verify safety and isolate the system.
                        2. Inspect pressure, relay state and shutdown cause.
                        3. Follow the operational checklist.
                        4. Restart locally and monitor vibration.
                    """.trimIndent(),
                    metrics = GenerationMetrics(
                        latencyMillis = 900,
                        timeToFirstTokenMillis = 120,
                        tokensGenerated = 64,
                        tokensPerSecond = 12,
                        memoryUsageMb = 256,
                    ),
                    model = request.model,
                    backend = request.backend,
                    generatedAt = "timestamp",
                    runtimeInfo = GenerationRuntimeInfo(
                        mode = RuntimeMode.Simulated,
                        inferenceLatencyMillis = 900,
                        totalExecutionTimeMillis = 950,
                    ),
                )
        }
}

