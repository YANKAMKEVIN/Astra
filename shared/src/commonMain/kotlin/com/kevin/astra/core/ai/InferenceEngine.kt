package com.kevin.astra.core.ai

interface InferenceEngine {
    suspend fun generate(request: PromptRequest): GenerationResult
}
