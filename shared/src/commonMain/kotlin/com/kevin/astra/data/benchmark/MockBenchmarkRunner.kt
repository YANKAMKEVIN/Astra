package com.kevin.astra.data.benchmark

import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.domain.benchmark.BenchmarkRecommendation
import com.kevin.astra.domain.benchmark.BenchmarkReport
import com.kevin.astra.domain.benchmark.BenchmarkRequest
import com.kevin.astra.domain.benchmark.BenchmarkResult
import com.kevin.astra.domain.benchmark.BenchmarkRunner
import com.kevin.astra.domain.benchmark.BenchmarkStatus
import kotlinx.coroutines.delay

class MockBenchmarkRunner : BenchmarkRunner {
    override suspend fun run(request: BenchmarkRequest): BenchmarkReport {
        delay(1_200)

        val results = request.models.distinct().map { model ->
            model.toMockResult(request.backend)
        }
        val recommended = results.sortedWith(
            compareByDescending<BenchmarkResult> { it.qualityScore }
                .thenByDescending { it.tokensPerSecond }
                .thenBy { it.latencyMillis }
                .thenBy { it.memoryUsageMb },
        ).firstOrNull()

        return BenchmarkReport(
            results = results,
            recommendation = recommended?.let {
                BenchmarkRecommendation(
                    model = it.model,
                    explanation = "${it.model.label} has the strongest simulated balance of quality, throughput, latency and memory usage for this prompt.",
                )
            },
        )
    }

    private fun AiModel.toMockResult(backend: InferenceBackend): BenchmarkResult {
        val profile = when (this) {
            AiModel.Mock -> MockProfile(1_200, 320, 18, 384, 74)
            AiModel.Gemma -> MockProfile(1_680, 410, 24, 720, 88)
            AiModel.Phi -> MockProfile(1_420, 360, 28, 640, 84)
            AiModel.Llama -> MockProfile(2_100, 520, 20, 920, 91)
            AiModel.Qwen -> MockProfile(1_760, 450, 26, 780, 89)
        }

        return BenchmarkResult(
            model = this,
            backend = backend,
            latencyMillis = profile.latencyMillis,
            timeToFirstTokenMillis = profile.timeToFirstTokenMillis,
            tokensPerSecond = profile.tokensPerSecond,
            memoryUsageMb = profile.memoryUsageMb,
            qualityScore = profile.qualityScore,
            status = BenchmarkStatus.Simulated,
        )
    }
}

private data class MockProfile(
    val latencyMillis: Long,
    val timeToFirstTokenMillis: Long,
    val tokensPerSecond: Int,
    val memoryUsageMb: Int,
    val qualityScore: Int,
)
