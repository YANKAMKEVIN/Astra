package com.kevin.astra.presentation.assistant

import androidx.lifecycle.viewModelScope
import com.kevin.astra.core.ai.AiModel
import com.kevin.astra.core.ai.GenerationResult
import com.kevin.astra.core.ai.InferenceBackend
import com.kevin.astra.core.ai.PromptRequest
import com.kevin.astra.core.mvi.AstraViewModel
import com.kevin.astra.domain.assistant.AskLocalAssistantUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AssistantViewModel(
    private val askLocalAssistant: AskLocalAssistantUseCase,
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
            updateState {
                copy(
                    isGenerating = true,
                    response = null,
                    generationTimestamp = null,
                )
            }

            val result = askLocalAssistant(
                PromptRequest(
                    prompt = snapshot.question,
                    industry = snapshot.selectedIndustry.toPromptIndustry(),
                    model = AiModel.Mock,
                    backend = InferenceBackend.Mock,
                    maxTokens = 512,
                    temperature = 0.2,
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
