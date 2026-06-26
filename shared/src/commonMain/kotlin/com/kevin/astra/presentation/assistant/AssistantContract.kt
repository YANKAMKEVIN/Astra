package com.kevin.astra.presentation.assistant

import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.core.mvi.AstraEffect
import com.kevin.astra.core.mvi.AstraIntent
import com.kevin.astra.core.mvi.AstraState

enum class AssistantIndustry(val label: String) {
    IndustrialMaintenance("Industrial Maintenance"),
    Aerospace("Aerospace"),
    Defense("Defense"),
    Energy("Energy"),
    Healthcare("Healthcare"),
}

fun AssistantIndustry.toPromptIndustry(): PromptIndustry = when (this) {
    AssistantIndustry.IndustrialMaintenance -> PromptIndustry.IndustrialMaintenance
    AssistantIndustry.Aerospace -> PromptIndustry.Aerospace
    AssistantIndustry.Defense -> PromptIndustry.Defense
    AssistantIndustry.Energy -> PromptIndustry.Energy
    AssistantIndustry.Healthcare -> PromptIndustry.Healthcare
}

data class AssistantMetrics(
    val model: String = "Mock Model",
    val backend: String = "Mock Engine",
    val latency: String = "1.2 s",
    val tokensPerSecond: String = "18",
    val timeToFirstToken: String = "320 ms",
    val tokensGenerated: String = "0",
    val memoryUsage: String = "384 MB",
)

data class AssistantResponse(
    val title: String,
    val body: String,
)

data class AssistantState(
    val selectedIndustry: AssistantIndustry = AssistantIndustry.IndustrialMaintenance,
    val question: String = "",
    val response: AssistantResponse? = null,
    val isGenerating: Boolean = false,
    val generationTimestamp: String? = null,
    val metrics: AssistantMetrics = AssistantMetrics(),
) : AstraState {
    val canAsk: Boolean
        get() = question.isNotBlank() && !isGenerating
}

sealed interface AssistantIntent : AstraIntent {
    data class UpdateQuestion(val question: String) : AssistantIntent
    data class SelectIndustry(val industry: AssistantIndustry) : AssistantIntent
    data object AskQuestion : AssistantIntent
    data object ClearConversation : AssistantIntent
}

sealed interface AssistantEffect : AstraEffect {
    data class ShowError(val message: String) : AssistantEffect
}
