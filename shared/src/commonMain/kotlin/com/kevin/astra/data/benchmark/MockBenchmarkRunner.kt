package com.kevin.astra.data.benchmark

import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.domain.benchmark.BenchmarkRecommendation
import com.kevin.astra.domain.benchmark.BenchmarkReport
import com.kevin.astra.domain.benchmark.BenchmarkRequest
import com.kevin.astra.domain.benchmark.BenchmarkResult
import com.kevin.astra.domain.benchmark.BenchmarkRunner
import com.kevin.astra.domain.benchmark.BenchmarkStatus
import com.kevin.astra.domain.evaluation.RuleBasedTaskEvaluationEngine
import com.kevin.astra.domain.evaluation.TaskEvaluationEngine
import kotlinx.coroutines.delay

class MockBenchmarkRunner(
    private val taskEvaluationEngine: TaskEvaluationEngine = RuleBasedTaskEvaluationEngine(),
) : BenchmarkRunner {
    override suspend fun run(request: BenchmarkRequest): BenchmarkReport {
        delay(1_200)

        val results = request.models.distinct().map { model ->
            model.toMockResult(request)
        }
        val recommended = results.sortedWith(
            compareByDescending<BenchmarkResult> { it.taskEvaluation.overallScore }
                .thenByDescending { it.tokensPerSecond }
                .thenBy { it.latencyMillis }
                .thenBy { it.memoryUsageMb },
        ).firstOrNull()

        return BenchmarkReport(
            results = results,
            recommendation = recommended?.let {
                BenchmarkRecommendation(
                    model = it.model,
                    explanation = "${it.model.displayName} has the strongest task evaluation score (${it.taskEvaluation.overallScore}/100) with a good simulated balance of throughput, latency and memory usage.",
                )
            },
        )
    }

    private fun LocalModel.toMockResult(request: BenchmarkRequest): BenchmarkResult {
        val profile = when (id) {
            "mock-model" -> MockProfile(1_200, 320, 18, 384, "Verify safety conditions, inspect pressure, follow a short restart checklist and monitor the equipment.")
            "gemma-3-1b" -> MockProfile(1_680, 410, 24, 720, "1. Isolate the asset and verify safety. 2. Inspect pressure, sensor state and shutdown cause. 3. Follow the approved operational checklist. 4. Restart locally and monitor vibration, temperature and alarms. 5. Log results and escalate if readings drift.")
            "phi-3-mini" -> MockProfile(1_420, 360, 28, 640, "1. Confirm the emergency state. 2. Check the equipment. 3. Restart if safe. 4. Monitor readings and document the action.")
            "llama-3-2-3b" -> MockProfile(2_100, 520, 20, 920, "1. Stop and isolate the system. 2. Verify lockout, pressure, temperature, alarms and operator authorization. 3. Inspect mechanical, electrical and sensor causes. 4. Execute the approved checklist step by step. 5. Keep local supervision active, monitor telemetry and record the maintenance decision.")
            "qwen-2-5-1-5b" -> MockProfile(1_760, 450, 26, 780, "1. Verify safety and isolate the affected subsystem. 2. Inspect pressure, relay state and abnormal sensor readings. 3. Apply the procedure checklist. 4. Restart under controlled conditions and monitor telemetry.")
            else -> MockProfile(1_900, 480, 16, minimumMemoryMb, "Review the task, apply safe checks and document the result.")
        }
        val evaluation = taskEvaluationEngine.evaluate(
            prompt = request.prompt,
            response = profile.response,
            industry = request.industry,
        )

        return BenchmarkResult(
            model = this,
            backend = request.backend,
            latencyMillis = profile.latencyMillis,
            timeToFirstTokenMillis = profile.timeToFirstTokenMillis,
            tokensPerSecond = profile.tokensPerSecond,
            memoryUsageMb = profile.memoryUsageMb,
            taskEvaluation = evaluation,
            status = BenchmarkStatus.Simulated,
        )
    }
}

private data class MockProfile(
    val latencyMillis: Long,
    val timeToFirstTokenMillis: Long,
    val tokensPerSecond: Int,
    val memoryUsageMb: Int,
    val response: String,
)
