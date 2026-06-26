package com.kevin.astra.presentation.benchmark

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.domain.benchmark.BenchmarkRequest
import com.kevin.astra.domain.benchmark.BenchmarkRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class BenchmarkViewModel(
    private val benchmarkRunner: BenchmarkRunner,
    private val benchmarkScope: CoroutineScope? = null,
) : AstraViewModel<BenchmarkState, BenchmarkIntent, BenchmarkEffect>(
    initialState = BenchmarkState(),
) {
    override fun handleIntent(intent: BenchmarkIntent) {
        when (intent) {
            is BenchmarkIntent.UpdatePrompt -> updateState {
                copy(prompt = intent.prompt, error = null)
            }

            is BenchmarkIntent.ToggleModel -> updateState {
                val nextModels = if (intent.model in selectedModels) {
                    selectedModels - intent.model
                } else {
                    selectedModels + intent.model
                }
                copy(selectedModels = nextModels, error = null)
            }

            is BenchmarkIntent.SelectBackend -> {
                if (intent.backend == InferenceBackend.Mock) {
                    updateState { copy(selectedBackend = intent.backend, error = null) }
                }
            }

            BenchmarkIntent.RunBenchmark -> runBenchmark()
        }
    }

    private fun runBenchmark() {
        val snapshot = state.value
        if (!snapshot.canRun) {
            updateState {
                copy(error = "Select at least one model and enter a benchmark prompt.")
            }
            return
        }

        (benchmarkScope ?: viewModelScope).launch {
            updateState {
                copy(
                    isRunning = true,
                    error = null,
                    results = emptyList(),
                    recommendedModel = null,
                )
            }

            val report = benchmarkRunner.run(
                BenchmarkRequest(
                    prompt = snapshot.prompt,
                    models = snapshot.selectedModels.toList(),
                    backend = snapshot.selectedBackend,
                ),
            )

            updateState {
                copy(
                    isRunning = false,
                    results = report.results,
                    recommendedModel = report.recommendation,
                )
            }
        }
    }
}
