package com.kevin.astra.data.benchmark

import com.kevin.astra.core.ai.GenerationResult
import com.kevin.astra.core.ai.InferenceEngine
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.PromptRequest
import com.kevin.astra.core.ai.RuntimeMode
import com.kevin.astra.domain.benchmark.BenchmarkRecommendation
import com.kevin.astra.domain.benchmark.BenchmarkReport
import com.kevin.astra.domain.benchmark.BenchmarkRequest
import com.kevin.astra.domain.benchmark.BenchmarkResult
import com.kevin.astra.domain.benchmark.BenchmarkRunner
import com.kevin.astra.domain.benchmark.BenchmarkStatus
import com.kevin.astra.domain.evaluation.RuleBasedTaskEvaluationEngine
import com.kevin.astra.domain.evaluation.TaskEvaluationEngine

class RuntimeBenchmarkRunner(
    private val inferenceEngine: InferenceEngine,
    private val taskEvaluationEngine: TaskEvaluationEngine = RuleBasedTaskEvaluationEngine(),
) : BenchmarkRunner {
    override suspend fun run(request: BenchmarkRequest): BenchmarkReport {
        val results = request.models.distinct().map { model ->
            val generation = inferenceEngine.generate(
                PromptRequest(
                    prompt = request.prompt,
                    systemPrompt = request.systemPrompt,
                    userMessage = request.userMessage,
                    industry = request.industry,
                    model = model.runtimeModel,
                    backend = request.backend,
                ),
            )
            model.toBenchmarkResult(
                request = request,
                generation = generation,
            )
        }
        val recommended = results.sortedWith(
            compareByDescending<BenchmarkResult> { it.taskEvaluation.overallScore }
                .thenByDescending { it.runtimeInfo.mode.realRuntimePreference() }
                .thenBy { it.runtimeInfo.totalExecutionTimeMillis.takeIf { value -> value > 0 } ?: Long.MAX_VALUE }
                .thenBy { it.memoryUsageMb ?: Int.MAX_VALUE },
        ).firstOrNull()

        return BenchmarkReport(
            results = results,
            recommendation = recommended?.let {
                BenchmarkRecommendation(
                    model = it.model,
                    explanation = "${it.model.displayName} leads with task score ${it.taskEvaluation.overallScore}/100, runtime mode ${it.runtimeInfo.mode.label}, and total time ${it.runtimeInfo.totalExecutionTimeMillis.toDisplayMillis()}.",
                )
            },
        )
    }

    private fun LocalModel.toBenchmarkResult(
        request: BenchmarkRequest,
        generation: GenerationResult,
    ): BenchmarkResult {
        val evaluation = taskEvaluationEngine.evaluate(
            prompt = request.prompt,
            response = generation.text,
            industry = request.industry,
        )

        return BenchmarkResult(
            model = this,
            selectedBackend = request.backend,
            usedBackend = generation.backend,
            runtimeInfo = generation.runtimeInfo,
            latencyMillis = generation.metrics.latencyMillis.availableMetric(),
            timeToFirstTokenMillis = generation.metrics.timeToFirstTokenMillis.availableMetric(),
            tokensPerSecond = generation.metrics.tokensPerSecond.availableMetric(),
            memoryUsageMb = generation.metrics.memoryUsageMb.availableMetric(),
            taskEvaluation = evaluation,
            status = if (generation.runtimeInfo.mode == RuntimeMode.Simulated) {
                BenchmarkStatus.Simulated
            } else {
                BenchmarkStatus.Completed
            },
        )
    }
}

private fun RuntimeMode.realRuntimePreference(): Int =
    when (this) {
        RuntimeMode.Real,
        RuntimeMode.LiteRtTensor,
        RuntimeMode.LiteRtLmGenerative,
        -> 2

        RuntimeMode.Simulated -> 1
        RuntimeMode.Fallback,
        RuntimeMode.ModelMissing,
        RuntimeMode.UnsupportedPlatform,
        -> 0
    }

private fun Long.availableMetric(): Long? =
    takeIf { it > 0L }

private fun Int.availableMetric(): Int? =
    takeIf { it > 0 }

private fun Long.toDisplayMillis(): String =
    if (this > 0L) "${this}ms" else "N/A"

