package com.kevin.astra.domain.assistant

import com.kevin.astra.core.ai.GenerationResult
import com.kevin.astra.core.ai.InferenceEngine
import com.kevin.astra.core.ai.PromptRequest
import kotlinx.coroutines.flow.Flow

class AskLocalAssistantUseCase(
    private val inferenceEngine: InferenceEngine,
) {
    suspend operator fun invoke(request: PromptRequest): GenerationResult =
        inferenceEngine.generate(request)

    fun stream(request: PromptRequest): Flow<StreamEvent> =
        inferenceEngine.generateStream(request)
}
