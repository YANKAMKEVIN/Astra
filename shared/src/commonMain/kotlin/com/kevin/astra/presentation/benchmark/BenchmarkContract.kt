package com.kevin.astra.presentation.benchmark

import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.mvi.AstraEffect
import com.kevin.astra.core.mvi.AstraIntent
import com.kevin.astra.core.mvi.AstraState
import com.kevin.astra.domain.benchmark.BenchmarkRecommendation
import com.kevin.astra.domain.benchmark.BenchmarkResult

data class BenchmarkState(
    val prompt: String = DefaultBenchmarkPrompt,
    val selectedModels: Set<AiModel> = setOf(AiModel.Mock, AiModel.Gemma, AiModel.Phi),
    val selectedBackend: InferenceBackend = InferenceBackend.Mock,
    val isRunning: Boolean = false,
    val results: List<BenchmarkResult> = emptyList(),
    val recommendedModel: BenchmarkRecommendation? = null,
    val error: String? = null,
) : AstraState {
    val canRun: Boolean
        get() = prompt.isNotBlank() && selectedModels.isNotEmpty() && !isRunning
}

sealed interface BenchmarkIntent : AstraIntent {
    data class UpdatePrompt(val prompt: String) : BenchmarkIntent
    data class ToggleModel(val model: AiModel) : BenchmarkIntent
    data class SelectBackend(val backend: InferenceBackend) : BenchmarkIntent
    data object RunBenchmark : BenchmarkIntent
}

sealed interface BenchmarkEffect : AstraEffect

const val DefaultBenchmarkPrompt: String =
    "How should an engineer restart Pump A after an emergency shutdown?"
