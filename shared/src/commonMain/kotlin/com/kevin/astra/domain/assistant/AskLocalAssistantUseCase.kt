package com.kevin.astra.domain.assistant

import com.kevin.astra.core.ai.GenerationResult
import com.kevin.astra.core.ai.InferenceEngine
import com.kevin.astra.core.ai.PromptRequest

class AskLocalAssistantUseCase(
    private val inferenceEngine: InferenceEngine,
) {
    suspend operator fun invoke(request: PromptRequest): GenerationResult =
        inferenceEngine.generate(request)
}
