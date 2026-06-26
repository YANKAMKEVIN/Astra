package com.kevin.astra.presentation.assistant

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.BackendCatalog
import com.kevin.astra.core.ai.GenerationResult
import com.kevin.astra.core.ai.ModelCatalog
import com.kevin.astra.core.ai.PromptBuildRequest
import com.kevin.astra.core.ai.PromptPipeline
import com.kevin.astra.core.ai.PromptRequest
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.domain.assistant.AskLocalAssistantUseCase
import com.kevin.astra.domain.settings.AiConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AssistantViewModel(
    private val askLocalAssistant: AskLocalAssistantUseCase,
    private val aiConfigurationRepository: AiConfigurationRepository,
    private val modelCatalog: ModelCatalog,
    private val backendCatalog: BackendCatalog,
    private val promptPipeline: PromptPipeline,
    private val generationScope: CoroutineScope? = null,
) : AstraViewModel<AssistantState, AssistantIntent, AssistantEffect>(
    initialState = AssistantState(),
) {
    override fun handleIntent(intent: AssistantIntent) {
        when (intent) {
            is AssistantIntent.UpdateQuestion -> updateState {
                copy(question = intent.question)
            }

            is AssistantIntent.SelectIndustry -> updateState {
                copy(selectedIndustry = intent.industry)
            }

            AssistantIntent.AskQuestion -> askQuestion()
            AssistantIntent.ClearConversation -> updateState {
                copy(
                    question = "",
                    response = null,
                    isGenerating = false,
                    generationTimestamp = null,
                )
            }
        }
    }

    private fun askQuestion() {
        val snapshot = state.value
        if (snapshot.isGenerating) return

        if (snapshot.question.isBlank()) {
            emitEffect(AssistantEffect.ShowError("Enter a critical operation question before asking ASTRA."))
            return
        }

        (generationScope ?: viewModelScope).launch {
            val configuration = aiConfigurationRepository.currentConfiguration.value
            val selectedModel = modelCatalog.currentModel()
            val selectedBackend = backendCatalog.currentBackend()
            val industry = snapshot.selectedIndustry.toPromptIndustry()
            val preparedPrompt = promptPipeline.preparePrompt(
                PromptBuildRequest(
                    engineerQuestion = snapshot.question,
                    selectedIndustry = industry,
                    selectedModel = selectedModel,
                ),
            )

            updateState {
                copy(
                    isGenerating = true,
                    response = null,
                    generationTimestamp = null,
                )
            }

            val result = askLocalAssistant(
                PromptRequest(
                    prompt = preparedPrompt,
                    industry = industry,
                    model = selectedModel.runtimeModel,
                    backend = selectedBackend.runtimeBackend,
                    maxTokens = configuration.maxTokens,
                    temperature = configuration.temperature,
                ),
            )

            updateState {
                copy(
                    isGenerating = false,
                    response = result.toAssistantResponse(),
                    generationTimestamp = result.generatedAt,
                    metrics = result.toAssistantMetrics(),
                )
            }
        }
    }
}

private fun GenerationResult.toAssistantResponse(): AssistantResponse =
    AssistantResponse(
        title = text.lineSequence().firstOrNull().orEmpty().ifBlank { "ASTRA response" },
        body = text,
    )

private fun GenerationResult.toAssistantMetrics(): AssistantMetrics =
    AssistantMetrics(
        model = model.label,
        backend = backend.label,
        latency = "${metrics.latencyMillis / 1_000.0} s",
        tokensPerSecond = metrics.tokensPerSecond.toString(),
        timeToFirstToken = "${metrics.timeToFirstTokenMillis} ms",
        tokensGenerated = metrics.tokensGenerated.toString(),
        memoryUsage = "${metrics.memoryUsageMb} MB",
    )
