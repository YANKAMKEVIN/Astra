package com.kevin.astra.presentation.assistant

import com.kevin.astra.core.ai.LocalModel
import com.kevin.astra.core.ai.PromptIndustry
import com.kevin.astra.core.mvi.AstraEffect
import com.kevin.astra.core.mvi.AstraIntent
import com.kevin.astra.core.mvi.AstraState
import com.kevin.astra.domain.assistant.PromptTemplate
import com.kevin.astra.domain.demo.DemoScenario

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

fun PromptIndustry.toAssistantIndustry(): AssistantIndustry = when (this) {
    PromptIndustry.IndustrialMaintenance -> AssistantIndustry.IndustrialMaintenance
    PromptIndustry.Aerospace -> AssistantIndustry.Aerospace
    PromptIndustry.Defense -> AssistantIndustry.Defense
    PromptIndustry.Energy -> AssistantIndustry.Energy
    PromptIndustry.Healthcare -> AssistantIndustry.Healthcare
}

data class AssistantMetrics(
    val model: String = "—",
    val backend: String = "—",
    val runtimeMode: String = "Pending",
    val latency: String = "—",
    val tokensPerSecond: String = "—",
    val timeToFirstToken: String = "—",
    val tokensGenerated: String = "—",
    val memoryUsage: String = "—",
    val modelLoadTime: String = "—",
    val totalExecutionTime: String = "—",
    val fallbackReason: String? = null,
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
    val availableScenarios: List<DemoScenario> = emptyList(),
    val promptTemplates: List<PromptTemplate> = emptyList(),
    val activeTemplate: PromptTemplate? = null,
    val installedModels: List<LocalModel> = emptyList(),
    val sessionModel: LocalModel? = null,
    val error: String? = null,
) : AstraState {
    val canAsk: Boolean
        get() = question.isNotBlank() && !isGenerating
}

sealed interface AssistantIntent : AstraIntent {
    data class UpdateQuestion(val question: String) : AssistantIntent
    data class SelectIndustry(val industry: AssistantIndustry) : AssistantIntent
    data class SelectScenario(val scenario: DemoScenario) : AssistantIntent
    data class SelectTemplate(val template: PromptTemplate) : AssistantIntent
    data class SelectSessionModel(val modelId: String) : AssistantIntent
    data object AskQuestion : AssistantIntent
    data object ClearConversation : AssistantIntent
}

sealed interface AssistantEffect : AstraEffect {
    data class ShowError(val message: String) : AssistantEffect
}
