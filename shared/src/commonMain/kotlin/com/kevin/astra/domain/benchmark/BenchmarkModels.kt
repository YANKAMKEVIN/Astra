package com.kevin.astra.domain.benchmark

import com.kevin.astra.core.ai.GenerationRuntimeInfo
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.domain.evaluation.TaskEvaluationReport

data class BenchmarkRequest(
    val prompt: String,
    val systemPrompt: String = "",
    val userMessage: String = "",
    val models: List<LocalModel>,
    val backend: InferenceBackend,
    val industry: PromptIndustry? = null,
)

data class BenchmarkReport(
    val results: List<BenchmarkResult>,
    val recommendation: BenchmarkRecommendation?,
)

data class BenchmarkResult(
    val model: LocalModel,
    val selectedBackend: InferenceBackend,
    val usedBackend: InferenceBackend,
    val runtimeInfo: GenerationRuntimeInfo,
    val latencyMillis: Long?,
    val timeToFirstTokenMillis: Long?,
    val tokensPerSecond: Int?,
    val memoryUsageMb: Int?,
    val taskEvaluation: TaskEvaluationReport,
    val status: BenchmarkStatus,
    val hardwareBefore: HardwareSnapshot? = null,
    val hardwareAfter: HardwareSnapshot? = null,
) {
    val backend: InferenceBackend
        get() = usedBackend

    val batteryDrainPercent: Int?
        get() {
            val before = hardwareBefore?.batteryPercent?.takeIf { it >= 0 } ?: return null
            val after = hardwareAfter?.batteryPercent?.takeIf { it >= 0 } ?: return null
            return (before - after).coerceAtLeast(0)
        }

    val temperatureDeltaCelsius: Float?
        get() {
            val before = hardwareBefore?.temperatureCelsius?.takeIf { it >= 0f } ?: return null
            val after = hardwareAfter?.temperatureCelsius?.takeIf { it >= 0f } ?: return null
            return after - before
        }

    val peakTemperatureCelsius: Float?
        get() = hardwareAfter?.temperatureCelsius?.takeIf { it >= 0f }

    val onDeviceCo2Mg: Double?
        get() = batteryDrainPercent?.let { Co2Estimator.onDeviceMg(it) }

    val cloudEquivalentCo2Mg: Double?
        get() = tokensPerSecond?.let { Co2Estimator.cloudEquivalentMg(it * 10) }

    val co2SavingsPercent: Int?
        get() {
            val onDevice = onDeviceCo2Mg ?: return null
            val cloud = cloudEquivalentCo2Mg ?: return null
            return Co2Estimator.savingsPercent(onDevice, cloud)
        }
}

enum class BenchmarkStatus(val label: String) {
    Completed("Completed"),
    Simulated("Simulated"),
}

data class BenchmarkRecommendation(
    val model: LocalModel,
    val explanation: String,
)
