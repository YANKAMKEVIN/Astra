package com.kevin.astra.domain.benchmark

import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.InferenceBackend

data class BenchmarkRequest(
    val prompt: String,
    val models: List<AiModel>,
    val backend: InferenceBackend,
)

data class BenchmarkReport(
    val results: List<BenchmarkResult>,
    val recommendation: BenchmarkRecommendation?,
)

data class BenchmarkResult(
    val model: AiModel,
    val backend: InferenceBackend,
    val latencyMillis: Long,
    val timeToFirstTokenMillis: Long,
    val tokensPerSecond: Int,
    val memoryUsageMb: Int,
    val qualityScore: Int,
    val status: BenchmarkStatus,
)

enum class BenchmarkStatus(val label: String) {
    Completed("Completed"),
    Simulated("Simulated"),
}

data class BenchmarkRecommendation(
    val model: AiModel,
    val explanation: String,
)
