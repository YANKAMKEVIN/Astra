package com.kevin.astra.domain.benchmark

import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.domain.evaluation.TaskEvaluationReport

data class BenchmarkRequest(
    val prompt: String,
    val models: List<LocalModel>,
    val backend: InferenceBackend,
    val industry: PromptIndustry,
)

data class BenchmarkReport(
    val results: List<BenchmarkResult>,
    val recommendation: BenchmarkRecommendation?,
)

data class BenchmarkResult(
    val model: LocalModel,
    val backend: InferenceBackend,
    val latencyMillis: Long,
    val timeToFirstTokenMillis: Long,
    val tokensPerSecond: Int,
    val memoryUsageMb: Int,
    val taskEvaluation: TaskEvaluationReport,
    val status: BenchmarkStatus,
)

enum class BenchmarkStatus(val label: String) {
    Completed("Completed"),
    Simulated("Simulated"),
}

data class BenchmarkRecommendation(
    val model: LocalModel,
    val explanation: String,
)
