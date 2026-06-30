package com.kevin.astra.core.ai

import com.kevin.astra.domain.assistant.StreamEvent
import kotlinx.coroutines.flow.Flow

interface InferenceEngine {
    suspend fun generate(request: PromptRequest): GenerationResult
    fun generateStream(request: PromptRequest): Flow<StreamEvent>
}
