package com.kevin.astra.presentation.benchmark

import com.kevin.astra.core.ai.InferenceBackendInfo
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.mvi.AstraEffect
import com.kevin.astra.core.mvi.AstraIntent
import com.kevin.astra.core.mvi.AstraState
import com.kevin.astra.domain.benchmark.BenchmarkRecommendation
import com.kevin.astra.domain.benchmark.BenchmarkResult
import com.kevin.astra.domain.demo.DemoScenario

data class BenchmarkState(
    val prompt: String = DefaultBenchmarkPrompt,
    val availableModels: List<LocalModel> = emptyList(),
    val selectedModelIds: Set<String> = emptySet(),
    val availableBackends: List<InferenceBackendInfo> = emptyList(),
    val selectedBackend: InferenceBackendInfo? = null,
    val isRunning: Boolean = false,
    val results: List<BenchmarkResult> = emptyList(),
    val recommendedModel: BenchmarkRecommendation? = null,
    val error: String? = null,
    val availableScenarios: List<DemoScenario> = emptyList(),
) : AstraState {
    val canRun: Boolean
        get() = prompt.isNotBlank() && selectedModelIds.isNotEmpty() && !isRunning
}

sealed interface BenchmarkIntent : AstraIntent {
    data class UpdatePrompt(val prompt: String) : BenchmarkIntent
    data class ToggleModel(val modelId: String) : BenchmarkIntent
    data class SelectBackend(val backendId: String) : BenchmarkIntent
    data class SelectScenario(val scenario: DemoScenario) : BenchmarkIntent
    data object RunBenchmark : BenchmarkIntent
}

sealed interface BenchmarkEffect : AstraEffect

const val DefaultBenchmarkPrompt: String =
    "How should an engineer restart Pump A after an emergency shutdown?"
