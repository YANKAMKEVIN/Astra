package com.kevin.astra.presentation.demo

import com.kevin.astra.core.ai.InferenceBackendInfo
import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.core.device.DeviceCapabilities
import com.kevin.astra.core.mvi.AstraEffect
import com.kevin.astra.core.mvi.AstraIntent
import com.kevin.astra.core.mvi.AstraState
import com.kevin.astra.domain.modelmanager.ModelReadiness

enum class DemoStep(val label: String, val description: String) {
    DeviceCapabilities("Device capabilities", "Confirm local platform and offline support."),
    RuntimeSelection("Runtime selection", "Review selected runtime and backend status."),
    Assistant("Assistant", "Run an operational prompt through ASTRA."),
    DocumentsAssistant("Documents Assistant", "Show embedded procedure retrieval."),
    Benchmark("Benchmark", "Compare model/runtime behavior."),
    TaskEvaluation("Task Evaluation", "Explain answer quality scoring."),
    ModelManager("Model Manager", "Inspect model files and fallback guidance."),
}

enum class DemoReadinessStatus(val label: String) {
    Ready("Ready"),
    Warning("Warning"),
    Blocked("Blocked"),
}

data class DemoReadinessIndicator(
    val label: String,
    val status: DemoReadinessStatus,
    val message: String,
)

data class DemoState(
    val capabilities: DeviceCapabilities? = null,
    val isLoadingCapabilities: Boolean = true,
    val selectedModel: LocalModel? = null,
    val selectedBackend: InferenceBackendInfo? = null,
    val selectedIndustry: PromptIndustry = PromptIndustry.IndustrialMaintenance,
    val modelReadiness: List<ModelReadiness> = emptyList(),
    val readinessIndicators: List<DemoReadinessIndicator> = emptyList(),
    val currentStep: DemoStep = DemoStep.DeviceCapabilities,
    val completedSteps: Set<DemoStep> = emptySet(),
    val error: String? = null,
) : AstraState {
    val isDemoReady: Boolean =
        readinessIndicators.isNotEmpty() && readinessIndicators.all { it.status == DemoReadinessStatus.Ready }

    val progressPercent: Int =
        ((completedSteps.size + 1).coerceAtMost(DemoStep.entries.size) * 100) / DemoStep.entries.size
}

sealed interface DemoIntent : AstraIntent {
    data object Refresh : DemoIntent
    data object NextStep : DemoIntent
    data object PreviousStep : DemoIntent
    data class SelectStep(val step: DemoStep) : DemoIntent
}

sealed interface DemoEffect : AstraEffect
